/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xxlabaza.test.rabbit.consumer;

import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;

import java.util.stream.Stream;

import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.ClientParameters;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
class RabbitConfiguration {

  @Value("${spring.rabbitmq.custom.api-addresses:}")
  String apiAddresses;

  @Autowired
  RabbitProperties rabbitProperties;

  @Bean
  SimpleMessageListenerContainer queuesListener (ConnectionFactory connectionFactory, MessageListener messageListener) {
    val container = new SimpleMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    // container.setQueueNames(queue().getName());
    container.setMessageListener(messageListener);
    container.setChannelTransacted(true);
    container.setDefaultRequeueRejected(false);
    return container;
  }

  @Bean
  MessageListener messageListener (ConsumerService consumer) {
    return new MessageListenerAdapter(consumer);
  }

  @Bean
  @ConditionalOnMissingClass("org.springframework.orm.jpa.JpaTransactionManager")
  RabbitTransactionManager rabbitTransactionManager (ConnectionFactory connectionFactory) {
    return new RabbitTransactionManager(connectionFactory);
  }

  @Bean
  RabbitRestClient rabbitRestClient () {
    val clients =  ofNullable(apiAddresses)
        .map(String::trim)
        .filter(not(String::isEmpty))
        .map(this::toHostPortStream)
        .orElseGet(() -> getDefaultApiHostPortStream())
        .map(this::createClient)
        .collect(toList());

    if (clients.isEmpty()) {
      throw new IllegalStateException("there are no available RabbitMQ REST API clients");
    }

    val result = new RabbitRestClient(clients);
    result.getAllQueueNames(); // check nodes availability
    return result;
  }

  private Stream<String> toHostPortStream (String addresses) {
    return Stream.of(addresses.split("\\,"))
        .map(String::trim)
        .filter(not(String::isEmpty));
  }

  private Stream<String> getDefaultApiHostPortStream () {
    val addresses = rabbitProperties.determineAddresses();
    return toHostPortStream(addresses)
        .map(it -> it.split("\\:")[0])
        .map(it -> it + ":15672");
  }

  @SneakyThrows
  private Client createClient (String hostPort) {
    val parameters = new ClientParameters()
        .url("http://" + hostPort + "/api")
        .username(rabbitProperties.getUsername())
        .password(rabbitProperties.getPassword());

    return new Client(parameters);
  }
}
