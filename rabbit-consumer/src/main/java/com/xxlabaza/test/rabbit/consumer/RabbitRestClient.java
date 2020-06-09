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

import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.domain.QueueInfo;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
class RabbitRestClient {

  Queue<Client> clients;

  AtomicReference<Client> current;

  @SuppressWarnings("PMD.AvoidUsingVolatile")
  volatile boolean flag;

  RabbitRestClient (List<Client> clients) {
    Collections.shuffle(clients);
    this.clients = new LinkedList<>(clients);
    current = new AtomicReference<>(this.clients.poll());
  }

  List<String> getAllQueueNames () {
    return withFallback(() ->
        current.get()
            .getQueues()
            .stream()
            .map(QueueInfo::getName)
            .collect(toList())
    );
  }

  private <T> T withFallback (Supplier<T> supplier) {
    for (int i = 0; i <= clients.size(); i++) {
      try {
        return supplier.get();
      } catch (Exception ex) {
        log.warn("Rabbit Management API error", ex);
        swapClient();
        continue;
      }
    }
    throw new IllegalStateException("No instances are available to serve the request");
  }

  private void swapClient () {
    val copy = flag;
    synchronized (this) {
      if (copy != flag) {
        return;
      }
      clients.add(current.get());
      current.set(clients.poll());

      flag = !flag;
    }
  }
}
