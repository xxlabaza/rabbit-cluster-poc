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

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
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
  @SuppressWarnings({
      "PMD.AvoidLiteralsInIfCondition",
      "PMD.DoNotCallSystemExit"
  })
  public void run (String... args) throws Exception {
    if (args.length != 1) {
      printUsage("Invalid number of the arguments");
      Runtime.getRuntime().exit(1);
    }

    switch (args[0].toLowerCase(ENGLISH)) {
    case "single":
      new SingleCommand().call();
      break;
    case "many":
      new ManyCommand().call();
      break;
    default:
      printUsage("Unknown command '" + args[0] + '\'');
      Runtime.getRuntime().exit(1);
    }
  }

  private void printUsage (String error) {
    if (error != null) {
      System.err.format(ENGLISH, "ERROR: %s%n%n", error);
    }
    System.out.println("USAGE:");
    System.out.println(" java -jar app.jar <command>\n");
    System.out.println("Available commands are:");
    System.out.println(" * single - for one queue load generating");
    System.out.println(" * many   - for creating 20 different queues");
  }

  private class SingleCommand implements Callable {

    @Override
    public Object call () throws Exception {
      log.info("sending 1_000_000 messages for one queue...");
      for (int i = 0; i < 1_000_000; i++) {
        val pushMessage = PushMessage.random(42);
        queuesService.send(pushMessage);
      }
      log.info("done");
      return null;
    }
  }

  private class ManyCommand implements Callable {

    @Override
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public Object call () throws Exception {
      val queues = 20;
      log.info("creating {} queues and sending messages", queues);

      val executor = Executors.newFixedThreadPool(queues);
      val countDownLatch = new CountDownLatch(queues);
      for (int i = 0; i < queues; i++) {
        executor.execute(new SendAction(countDownLatch));
      }

      countDownLatch.await();
      log.info("done");
      return null;
    }

    private class SendAction implements Runnable {

      CountDownLatch countDownLatch;

      PushMessage pushMessage = PushMessage.random().withId(1);

      SendAction (CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
      }

      @Override
      public void run () {
        log.info("start sending messages for channel={}, type={}",
                 pushMessage.getChannel(), pushMessage.getType());

        for (int i = 0; i < 50_000; i++) {
          try {
            queuesService.send(pushMessage);
            val random = ThreadLocalRandom.current().nextInt(50, 200);
            TimeUnit.MILLISECONDS.sleep(random);
          } catch (Exception ex) {
            log.error("sending {} error", pushMessage, ex);
          }
          pushMessage = pushMessage.withId(pushMessage.getId() + 1);
        }
        countDownLatch.countDown();
      }
    }
  }
}
