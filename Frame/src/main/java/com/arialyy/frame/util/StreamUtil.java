/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.arialyy.frame.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Created by AriaLyy on 2015/4/10.
 */
public class StreamUtil {
  /**
   * 得到图片字节流 数组大小
   */
  public static byte[] readStream(InputStream inStream) throws Exception {
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int len = 0;
    while ((len = inStream.read(buffer)) != -1) {
      outStream.write(buffer, 0, len);
    }
    outStream.close();
    return outStream.toByteArray();
  }
}