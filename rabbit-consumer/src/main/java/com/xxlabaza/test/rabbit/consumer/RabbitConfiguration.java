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

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.stream.Stream;

import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.ClientParameters;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
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
  RabbitRestClient rabbitRestClient () {
    List<Client> clients =  ofNullable(apiAddresses)
        .map(String::trim)
        .filter(not(String::isEmpty))
        .map(this::toHostPortStream)
        .orElseGet(() -> getDefaultApiHostPortStream())
        .map(this::createClient)
        .collect(toList());;

    return new RabbitRestClient(clients);
  }

  @Bean
  SimpleMessageListenerContainer queuesListener (ConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter) {
    val container = new SimpleMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    // container.setQueueNames(queue().getName());
    container.setMessageListener(listenerAdapter);
    return container;
  }

  @Bean
  MessageListenerAdapter listenerAdapter (ConsumerService consumer) {
    return new MessageListenerAdapter(consumer, "receive");
  }

  private Stream<String> toHostPortStream (String addresses) {
    return Stream.of(apiAddresses.split(","))
        .map(String::trim)
        .filter(not(String::isEmpty));
  }

  private Stream<String> getDefaultApiHostPortStream () {
    val addresses = rabbitProperties.determineAddresses();
    return toHostPortStream(addresses)
        .map(it -> it.split(":")[0])
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
