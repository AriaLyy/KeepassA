/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.arialyy.frame.base;

import androidx.lifecycle.ViewModel;
import com.arialyy.frame.base.net.NetManager;
import com.arialyy.frame.util.StringUtil;

/**
 * Created by AriaL on 2017/11/26.
 * ViewModule只能是public
 */

public class BaseViewModule extends ViewModel {
  protected NetManager mNetManager;
  protected String TAG = StringUtil.getClassName(this);

  public BaseViewModule() {
    mNetManager = NetManager.getInstance();
  }

}