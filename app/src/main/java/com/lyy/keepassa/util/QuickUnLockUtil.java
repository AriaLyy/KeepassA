/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util;

/**
 * 密码工具
 */
public class QuickUnLockUtil {
  /**
   * 快速解锁保存的信息
   */
  private static final String QUICK_UN_LOCK_FILE_NAME = "91b7b54da7f5154020a528";

  static {
    System.loadLibrary("keepassA");
  }

  /**
   * 加密字符串
   *
   * @param str 明文
   * @return 密文
   */
  //@Deprecated(message = "不安全")
  public native static String encryptStr(String str);

  /**
   * 解密字符串
   *
   * @param str 密文
   * @return 明文
   */
  public native static String decryption(String str);

  /**
   * 获取数据库密码
   */
  public native static String getDbPass();
}