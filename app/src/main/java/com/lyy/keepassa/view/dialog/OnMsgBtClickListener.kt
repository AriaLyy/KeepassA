/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.view.dialog;

import android.widget.Button
import java.io.Serializable

/**
 * @author laoyuyu
 * @date 2021/9/5
 */
interface OnMsgBtClickListener {

  fun onCover(v: Button)

  fun onEnter(v: Button)

  fun onCancel(v: Button)
}