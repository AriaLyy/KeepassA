/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.widget.editor;

/**
 * @Author laoyuyu
 * @Date 2020/11/30
 */
public interface OperateType {
  int OPERATE_UNKNOWN = 0;
  int OPERATE_ADD = 1;
  int OPERATE_DEL = 2;
  int OPERATE_CLEAR = 3;
  int OPERATE_UNDO = 4;
  int OPERATE_REDO = 5;
}