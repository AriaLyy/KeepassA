/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.feat

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Process
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.facade.template.IProvider
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.util.KeyStoreUtil.Companion.keyStorePass
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.Utils
import com.lyy.keepassa.KpaEventBusIndex
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.DbMigration.MIGRATION_2_3
import com.lyy.keepassa.base.DbMigration.MIGRATION_3_4
import com.lyy.keepassa.dao.AppDatabase
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.QuickUnLockUtil
import com.tencent.bugly.crashreport.CrashReport
import com.tencent.bugly.crashreport.CrashReport.UserStrategy
import com.tencent.mmkv.MMKV
import com.tencent.vasdolly.helper.ChannelReaderUtil
import com.tencent.wcdb.database.SQLiteCipherSpec
import com.tencent.wcdb.room.db.WCDBOpenHelperFactory
import com.zzhoujay.richtext.RichText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 3:24 下午 2022/4/15
 **/
@Route(path = "/service/kpaSdk")
class KpaSdkService : IProvider {
  private val scope = MainScope()

  fun preInitSdk(context: Application) {
    MMKV.initialize(context)
    Utils.init(context)
    RoomFeature.init(context)
    // 开启kotlin 协程debug
    if (AppUtils.isAppDebug()) {
      System.setProperty("kotlinx.coroutines.debug", "on")
      Timber.plant(Timber.DebugTree())
      ARouter.openLog() // 打印日志
      ARouter.openDebug() // 开启调试模式(如果在InstantRun模式下运行，必须开启调试模式！线上版本需要关闭,否则有安全风险)
    }
    scope.launch(Dispatchers.IO) {
      // 初始化一下时间
      KeepassAUtil.instance.isFastClick()
      keyStorePass = QuickUnLockUtil.getDbPass().toCharArray()
      val showStatusBar = PreferenceManager.getDefaultSharedPreferences(BaseApp.APP)
        .getBoolean(ResUtil.getString(R.string.set_key_title_show_state_bar), true)
      BaseActivity.showStatusBar = showStatusBar
      EventBus.builder().addIndex(KpaEventBusIndex()).installDefaultEventBus()
      listenerAppBackground()
    }
  }

  fun initThirdSdk(context: Context) {
    scope.launch(Dispatchers.IO) {
      BuglyFeature.init(context)
      RichText.initCacheDir(context)
      XLogFeature.init(context)
    }
  }

  private fun listenerAppBackground() {
    AppUtils.registerAppStatusChangedListener(object : Utils.OnAppStatusChangedListener {
      override fun onForeground(activity: Activity?) {
      }

      override fun onBackground(activity: Activity?) {
        KpaUtil.kdbHandlerService.saveDbByBackground(true)
        XLogFeature.flush()
      }
    })
  }

  override fun init(context: Context?) {
  }
}