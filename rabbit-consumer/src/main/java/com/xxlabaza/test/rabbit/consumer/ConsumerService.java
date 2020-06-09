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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
class ConsumerService {

  private static final Pattern QUEUE_NAME_PATTERN = Pattern.compile("ha.(?<type>\\w+)_(?<channel>\\d+)_queue");

  @Autowired
  @Qualifier("queuesListener")
  AbstractMessageListenerContainer queuesListener;

  @Autowired
  RabbitRestClient client;

  @Autowired
  ObjectMapper objectMapper;

  Map<Integer, Set<String>> queues = new ConcurrentHashMap<>();

  @SneakyThrows
  public void receive (byte[] bytes) {
    val pushMessage = objectMapper.readValue(bytes, PushMessage.class);
    log.info("inbound - {}", pushMessage);
  }

  @Scheduled(fixedRate = 5_000)
  void updateQueues () {
    val newQueueNames = client.getAllQueueNames()
        .stream()
        .filter(this::isNewQueue)
        .toArray(String[]::new);

    if (newQueueNames.length == 0) {
      return;
    }

    log.info("new queues {}", (Object) newQueueNames);
    queuesListener.addQueueNames(newQueueNames);
  }

  private boolean isNewQueue (String queueName) {
    val matcher = QUEUE_NAME_PATTERN.matcher(queueName);
    if (matcher.find() == false) {
      return false;
    }

    val type = matcher.group("type");
    val channel = Integer.parseInt(matcher.group("channel"), 10);

    return queues
        .computeIfAbsent(channel, key -> ConcurrentHashMap.newKeySet())
        .add(type);
  }
}
