/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.launcher

import android.content.Intent
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.entity.AutoFillParam

interface IAutoFillFinishDelegate {

  fun finish(activity: BaseActivity<*>, autoFillParam: AutoFillParam)

  fun onActivityResult(activity: BaseActivity<*>, data: Intent?)
}