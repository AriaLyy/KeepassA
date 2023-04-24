/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.AutoFillParam
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.view.create.CreateEntryActivity
import com.lyy.keepassa.view.search.AutoFillEntrySearchActivity
import timber.log.Timber

internal object CreateEntityFinishDelegate : IAutoFillFinishDelegate {

  private var autoFillParam: AutoFillParam? = null

  val content = object : ActivityResultContract<AutoFillParam, Intent?>() {
    override fun createIntent(context: Context, input: AutoFillParam): Intent {
      val intent = Intent(context, CreateEntryActivity::class.java).apply {
        putExtra(LauncherActivity.KEY_AUTO_FILL_PARAM, input)
      }
      return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Intent? {
      if (resultCode == Activity.RESULT_OK) {
        return intent
      }
      return null
    }
  }

  override fun finish(activity: BaseActivity<*>, autoFillParam: AutoFillParam) {
    this.autoFillParam = autoFillParam

    val reg = activity.registerForActivityResult(content) {
      onActivityResult(activity, it)
    }
    reg.launch(autoFillParam, ActivityOptionsCompat.makeSceneTransitionAnimation(activity))
  }

  override fun onActivityResult(activity: BaseActivity<*>, data: Intent?) {
    if (autoFillParam == null) {
      Timber.e("autoFillParam is null")
      return
    }

    data?.let {
      val id = data.getSerializableExtra(AutoFillEntrySearchActivity.EXTRA_ENTRY_ID)
      activity.setResult(
        Activity.RESULT_OK,
        BaseApp.KDB.pm.entries[id]?.let {
          KeepassAUtil.instance.getFillResponse(
            activity,
            activity.intent,
            it,
            autoFillParam!!.apkPkgName
          )
        }
      )
    }
  }
}