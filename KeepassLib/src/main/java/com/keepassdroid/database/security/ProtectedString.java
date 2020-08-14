/*
 * Copyright 2013 Brian Pellin.
 *
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
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
