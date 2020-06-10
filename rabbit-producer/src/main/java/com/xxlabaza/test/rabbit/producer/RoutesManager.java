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

package com.xxlabaza.test.rabbit.producer;

import static java.util.Locale.ENGLISH;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.NonNull;
import lombok.val;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
class RoutesManager {

  @Autowired
  AmqpAdmin admin;

  @Autowired
  @Qualifier("inboundMessagesExchange")
  TopicExchange inboundMessagesExchange;

  Map<String, Boolean> cache = new ConcurrentHashMap<>();

  String getRoutingKey (@NonNull PushMessage pushMessage) {
    val routingKey = generateRoutingKey(pushMessage);

    cache.computeIfAbsent(routingKey, key -> {
      createQueue(key, pushMessage);
      return Boolean.TRUE;
    });

    return routingKey;
  }

  private String generateRoutingKey (PushMessage pushMessage) {
    val channel = pushMessage.getChannel();
    val type = pushMessage.getType().name().toLowerCase(ENGLISH);
    return String.format(ENGLISH, "pushMessage.%d.%s", channel, type);
  }

  private void createQueue (String routingKey, PushMessage pushMessage) {
    val clientExchangeName = String.format(ENGLISH, "client_%d_exchange", pushMessage.getChannel());
    val clientExchange = (DirectExchange) ExchangeBuilder.directExchange(clientExchangeName)
        .durable(true)
        .build();

    val queueType = pushMessage.getType().name().toLowerCase(ENGLISH);

    val retryQueueName = String.format(ENGLISH, "ha.%s_%d_retry_queue", queueType, pushMessage.getChannel());
    val retryQueueRoutingKey = routingKey + ".retry";
    val retryQueue = QueueBuilder
        .durable(retryQueueName)
        .build();

    val queueName = String.format(ENGLISH, "ha.%s_%d_queue", queueType, pushMessage.getChannel());
    val queue = QueueBuilder
        .durable(queueName)
        .deadLetterExchange(clientExchangeName)
        .deadLetterRoutingKey(retryQueueRoutingKey)
        .build();

    val retryQueueBinding = BindingBuilder.bind(retryQueue).to(clientExchange).with(retryQueueRoutingKey);
    val queueBinding = BindingBuilder.bind(queue).to(clientExchange).with(routingKey);

    val topLevelRoutingKey = String.format(ENGLISH, "pushMessage.%d.#", pushMessage.getChannel());
    val exchangeBinding = BindingBuilder.bind(clientExchange).to(inboundMessagesExchange).with(topLevelRoutingKey);

    admin.declareExchange(clientExchange);
    admin.declareQueue(retryQueue);
    admin.declareQueue(queue);
    admin.declareBinding(queueBinding);
    admin.declareBinding(retryQueueBinding);
    admin.declareBinding(exchangeBinding);
  }
}
