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
import com.lyy.keepassa.util.KeepassAUtil.Companion.instance
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.QuickUnLockUtil
import com.tencent.bugly.crashreport.CrashReport
import com.tencent.bugly.crashreport.CrashReport.UserStrategy
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
    Utils.init(context)
    initDb(context)
    // 开启kotlin 协程debug
    if (AppUtils.isAppDebug()) {
      System.setProperty("kotlinx.coroutines.debug", "on")
      Timber.plant(Timber.DebugTree())
      ARouter.openLog() // 打印日志
      ARouter.openDebug() // 开启调试模式(如果在InstantRun模式下运行，必须开启调试模式！线上版本需要关闭,否则有安全风险)
    }
    keyStorePass = QuickUnLockUtil.getDbPass().toCharArray()
    val showStatusBar = PreferenceManager.getDefaultSharedPreferences(BaseApp.APP)
      .getBoolean(ResUtil.getString(R.string.set_key_title_show_state_bar), true)
    BaseActivity.showStatusBar = showStatusBar

    scope.launch(Dispatchers.IO) {
      initBugly(context)
      RichText.initCacheDir(context)
//      EventBus.builder().addIndex(KpaEventBusIndex()).installDefaultEventBus()
      listenerAppBackground()
    }
  }

  private fun listenerAppBackground(){
    AppUtils.registerAppStatusChangedListener(object :Utils.OnAppStatusChangedListener{
      override fun onForeground(activity: Activity?) {
      }

      override fun onBackground(activity: Activity?) {
        KpaUtil.kdbHandlerService.saveDbByBackground(true)
      }
    })
  }

  private fun initBugly(context: Context) {
    val kUtil = instance
    // 获取当前包名
    val packageName: String = context.packageName

    // 获取当前进程名
    val processName = kUtil.getProcessName(Process.myPid())
    val strategy = UserStrategy(context)
    strategy.isUploadProcess = processName == null || processName == packageName
    strategy.appChannel = getChannel(context)
    strategy.appVersion = kUtil.getAppVersionName(context)
    CrashReport.initCrashReport(
      context.applicationContext, "59fc0ec759", AppUtils.isAppDebug(),
      strategy
    )
    //CrashReport.testJavaCrash();
  }

  private fun getChannel(context: Context): String {
    var channel = ChannelReaderUtil.getChannel(context.applicationContext)
    if (channel == null) {
      channel = "default"
    }
    return channel
  }

  /**
   * 初始化数据库
   */
  private fun initDb(context: Context) {
    // 初始化数据库
    val cipherSpec = SQLiteCipherSpec() // 指定加密方式，使用默认加密可以省略
      .setPageSize(4096)
      .setKDFIteration(64000)
    val factory = WCDBOpenHelperFactory()
      .passphrase(QuickUnLockUtil.getDbPass().toByteArray()) // 指定加密DB密钥，非加密DB去掉此行
      .cipherSpec(cipherSpec) // 指定加密方式，使用默认加密可以省略
      .writeAheadLoggingEnabled(true) // 打开WAL以及读写并发，可以省略让Room决定是否要打开
      .asyncCheckpointEnabled(true) // 打开异步Checkpoint优化，不需要可以省略
    BaseApp.appDatabase = Room.databaseBuilder(
      context,
      AppDatabase::class.java, AppDatabase.DB_NAME
    )
      .openHelperFactory(factory)
      .addMigrations(MIGRATION_2_3(), MIGRATION_3_4())
      .build()
  }

  override fun init(context: Context?) {
  }
}