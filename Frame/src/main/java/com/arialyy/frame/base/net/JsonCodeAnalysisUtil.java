/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.arialyy.frame.base.net;

import com.google.gson.JsonObject;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by AriaL on 2017/11/26.
 */

public class JsonCodeAnalysisUtil {

  public static boolean isSuccess(JsonObject obj) {
    JSONObject object = null;
    try {
      object = new JSONObject(obj.toString());
      return object.optBoolean("success");
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return false;
  }
}