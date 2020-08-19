/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.arialyy.frame.permission;

/**
 * Created by lyy on 2016/4/11.
 * 权限回调
 */
public interface OnPermissionCallback {
  public static final int PERMISSION_ALERT_WINDOW = 0xad1;
  public static final int PERMISSION_WRITE_SETTING = 0xad2;

  public void onSuccess(String... permissions);

  public void onFail(String... permissions);
}