/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.arialyy.frame.core;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Window;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import com.arialyy.frame.util.StringUtil;

/**
 * Created by lyy on 2015/11/4.
 * 继承Dialog
 */
public abstract class AbsDialog<VB extends ViewDataBinding> extends Dialog {
  protected String TAG = StringUtil.getClassName(this);
  private VB mBind;

  public AbsDialog(Context context) {
    super(context);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    mBind = DataBindingUtil.inflate(LayoutInflater.from(getContext()), setLayoutId(), null, false);
    setContentView(mBind.getRoot());
  }

  protected void initDialog() {

  }

  @Override protected void onStart() {
    super.onStart();
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initDialog();
  }


  public VB getBinding() {
    return mBind;
  }

  /**
   * 设置资源布局
   */
  protected abstract int setLayoutId();
}