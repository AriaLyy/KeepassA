/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.arialyy.frame.cache;

import android.os.Environment;

/**
 * Created by AriaL on 2017/11/26.
 */

public class PathConstaant {
  private static final String WP_DIR = "windPath";

  /**
   * 获取APK升级路径
   */
  public static String getWpPath() {
    return Environment.getExternalStorageDirectory().getPath()
        + "/"
        + WP_DIR
        + "/update/windPath.apk";
  }
}