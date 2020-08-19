/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database.security;

public class ProtectedString {

  private String string;
  private boolean protect;
  /**
   * 是否是otp密码字段
   */
  private boolean isOtpPass = false;


  public boolean isProtected() {
    return protect;
  }

  public int length() {
    if (string == null) {
      return 0;
    }

    return string.length();
  }

  public ProtectedString() {
    this(false, "");
  }

  @Override public boolean equals(Object obj) {
    if (!(obj instanceof ProtectedString)) {
      return false;
    }
    ProtectedString other = (ProtectedString) obj;
    return other.protect == this.protect && other.string.equals(string);
  }

  public boolean isOtpPass() {
    return isOtpPass;
  }

  public void setOtpPass(boolean otpPass) {
    isOtpPass = otpPass;
  }

  public ProtectedString(boolean enableProtection, String string) {
    protect = enableProtection;
    this.string = string;
  }

  public String toString() {
    return string;
  }
}