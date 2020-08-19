/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.arialyy.frame.http.inf;

/**
 * 数据响应接口
 */
public interface IResponse {
  /**
   * 响应的数据回调
   */
  public void onResponse(String data);

  /**
   * 错误返回回掉
   */
  public void onError(Object error);
}