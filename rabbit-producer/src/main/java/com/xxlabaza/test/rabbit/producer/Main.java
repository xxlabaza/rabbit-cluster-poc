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

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class Main implements CommandLineRunner {

  public static void main (String[] args) {
    SpringApplication.run(Main.class, args);
  }

  @Autowired
  QueuesService queuesService;

  @Override
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  public void run (String... args) throws Exception {
    log.info("sending messages...");

    val executor = Executors.newCachedThreadPool();
    for (int i = 0; i < 10; i++) {
      executor.execute(new MyAction());
      TimeUnit.SECONDS.sleep(30);
    }

    log.info("messages were send");
  }

  private class MyAction implements Runnable {

    PushMessage pushMessage = PushMessage.random().withId(1);

    @Override
    public void run () {
      log.info("start sending messages for channel={}, type={}",
               pushMessage.getChannel(), pushMessage.getType());

      while (true) {
        try {
          queuesService.send(pushMessage);
          TimeUnit.SECONDS.sleep(10);
        } catch (Exception ex) {
          log.error("sending {} error", pushMessage, ex);
        }
        pushMessage = pushMessage.withId(pushMessage.getId() + 1);
      }
    }
  }
}
