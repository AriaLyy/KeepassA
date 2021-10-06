/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.compat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import java.lang.reflect.Field;
import keepass2android.kp2akeytransform.R;

/**
 * Created by bpellin on 3/10/16.
 */
public class StorageAF {

  public static String ACTION_OPEN_DOCUMENT;

  static {
    try {
      Field openDocument = Intent.class.getField("ACTION_OPEN_DOCUMENT");
      ACTION_OPEN_DOCUMENT = (String) openDocument.get(null);
    } catch (Exception e) {
      ACTION_OPEN_DOCUMENT = "android.intent.action.OPEN_DOCUMENT";
    }
  }

  public static boolean supportsStorageFramework() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
  }

  public static boolean useStorageFramework(Context ctx) {
    if (!supportsStorageFramework()) {
      return false;
    }

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    return prefs.getBoolean(ctx.getString(R.string.saf_key),
        ctx.getResources().getBoolean(R.bool.saf_default));
  }
}