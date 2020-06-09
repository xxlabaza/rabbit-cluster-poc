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

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.val;

@With
@Value
@Builder
class PushMessage {

  static PushMessage random () {
    val channel = ThreadLocalRandom.current().nextInt(1000, 9999);
    return random(channel);
  }

  static PushMessage random (int channel) {
    return PushMessage.builder()
        .id(ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE))
        .type(Type.random())
        .channel(channel)
        .payload(UUID.randomUUID().toString())
        .build();
  }

  int id;

  Type type;

  int channel;

  String payload;

  enum Type {

    DLR,
    HLR,
    MO;

    static Type random () {
      val types = values();
      val index = ThreadLocalRandom.current().nextInt(0, types.length);
      return types[index];
    }
  }
}
