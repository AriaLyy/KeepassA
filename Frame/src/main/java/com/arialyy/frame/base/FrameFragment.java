/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.arialyy.frame.base;

import androidx.databinding.ViewDataBinding;
import com.arialyy.frame.core.AbsFragment;

/**
 * Created by Aria.Lao on 2017/12/1.
 */
public abstract class FrameFragment<VB extends ViewDataBinding> extends AbsFragment<VB> {
  public int color;


  @Override protected void dataCallback(int result, Object obj) {

  }

  @Override protected void onDelayLoad() {

  }
}