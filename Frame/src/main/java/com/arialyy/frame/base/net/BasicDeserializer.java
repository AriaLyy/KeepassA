/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.arialyy.frame.base.net;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;

/**
 * 自定义Gson描述
 * Created by “Aria.Lao” on 2016/10/26.
 *
 * @param <T> 服务器数据实体
 */
public class BasicDeserializer<T> implements JsonDeserializer<T> {
  @Override
  public T deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject root = element.getAsJsonObject();
    if (JsonCodeAnalysisUtil.isSuccess(root)) {
      return new Gson().fromJson(root.get("object"), typeOfT);
    } else {
      throw new IllegalStateException(root.get("rltmsg").getAsString());
    }
  }
}