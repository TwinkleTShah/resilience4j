/*
 * Copyright 2019 Mahmoud Romeh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.springboot.common.circuitbreaker.autoconfigure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.configure.*;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.utils.ReactorOnClasspathCondition;
import io.github.resilience4j.utils.RxJava2OnClasspathCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public abstract class AbstractCircuitBreakerConfigurationOnMissingBean {

	protected final CircuitBreakerConfiguration circuitBreakerConfiguration;
	protected final CircuitBreakerConfigurationProperties circuitBreakerProperties;

	public AbstractCircuitBreakerConfigurationOnMissingBean(CircuitBreakerConfigurationProperties circuitBreakerProperties) {
		this.circuitBreakerProperties = circuitBreakerProperties;
		this.circuitBreakerConfiguration = new CircuitBreakerConfiguration(circuitBreakerProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	public CircuitBreakerRegistry circuitBreakerRegistry(EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry) {
        Map<String, CircuitBreakerConfig> configs = circuitBreakerProperties.getConfigs()
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> circuitBreakerProperties.createCircuitBreakerConfig(entry.getValue())));

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(configs);

		// Register the event consumers
		circuitBreakerConfiguration.registerEventConsumer(circuitBreakerRegistry, eventConsumerRegistry);
		// Register a consumer to hook up any health indicators for circuit breakers after creation. This will catch ones that get
		// created beyond initially configured backends.
		circuitBreakerRegistry.getEventPublisher().onEntryAdded(event -> createHealthIndicatorForCircuitBreaker(event.getAddedEntry()));

		// Initialize backends that were initially configured.
		circuitBreakerConfiguration.initializeBackends(circuitBreakerRegistry);

		return circuitBreakerRegistry;
	}

	protected abstract void createHealthIndicatorForCircuitBreaker(CircuitBreaker circuitBreaker);

	@Bean
	@ConditionalOnMissingBean
	public CircuitBreakerAspect circuitBreakerAspect(CircuitBreakerRegistry circuitBreakerRegistry,
	                                                 @Autowired(required = false) List<CircuitBreakerAspectExt> circuitBreakerAspectExtList) {
		return circuitBreakerConfiguration.circuitBreakerAspect(circuitBreakerRegistry, circuitBreakerAspectExtList);
	}

	@Bean
	@Conditional(value = {RxJava2OnClasspathCondition.class})
	@ConditionalOnMissingBean
	public RxJava2CircuitBreakerAspectExt rxJava2CircuitBreakerAspect() {
		return circuitBreakerConfiguration.rxJava2CircuitBreakerAspect();
	}

	@Bean
	@Conditional(value = {ReactorOnClasspathCondition.class})
	@ConditionalOnMissingBean
	public ReactorCircuitBreakerAspectExt reactorCircuitBreakerAspect() {
		return circuitBreakerConfiguration.reactorCircuitBreakerAspect();
	}

}
