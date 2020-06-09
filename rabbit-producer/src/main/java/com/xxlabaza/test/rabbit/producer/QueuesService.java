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

import lombok.val;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
class QueuesService {

  @Autowired
  RabbitTemplate rabbitTemplate;

  @Autowired
  @Qualifier("inboundMessagesExchange")
  TopicExchange inboundMessagesExchange;

  @Autowired
  RoutesManager routesManager;

  void send (PushMessage pushMessage) {
    val routingKey = routesManager.getRoutingKey(pushMessage);
    rabbitTemplate.convertAndSend(inboundMessagesExchange.getName(), routingKey, pushMessage);
  }
}
