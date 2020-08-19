/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.arialyy.frame.util;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * 打开或关闭软键盘
 *
 * @author zhy
 */
public class KeyBoardUtils {
  /**
   * 打卡软键盘
   *
   * @param mEditText 输入框
   * @param mContext 上下文
   */
  public static void openKeybord(EditText mEditText, Context mContext) {
    InputMethodManager imm = (InputMethodManager) mContext
        .getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.showSoftInput(mEditText, InputMethodManager.RESULT_SHOWN);
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,
        InputMethodManager.HIDE_IMPLICIT_ONLY);
  }

  /**
   * 关闭软键盘
   *
   * @param mEditText 输入框
   * @param mContext 上下文
   */
  public static void closeKeybord(EditText mEditText, Context mContext) {
    InputMethodManager imm = (InputMethodManager) mContext
        .getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
  }
}