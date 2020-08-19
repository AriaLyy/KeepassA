/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.arialyy.frame.base.net;

/**
 * Created by “Aria.Lao” on 2016/10/25.
 * 网络响应接口，所有的网络回调都要继承该接口
 *
 * @param <T> 数据实体结构
 */
public interface INetResponse<T> {

  /**
   * 网络请求成功
   */
  public void onResponse(T response);

  /**
   * 请求失败
   */
  public void onFailure(Throwable e);
}