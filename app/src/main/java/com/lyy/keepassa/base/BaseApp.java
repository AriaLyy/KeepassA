/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.base;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;
import androidx.preference.PreferenceManager;
import com.alibaba.android.arouter.launcher.ARouter;
import com.arialyy.frame.core.AbsFrame;
import com.arialyy.frame.router.Routerfit;
import com.keepassdroid.Database;
import com.lyy.keepassa.R;
import com.lyy.keepassa.common.PassType;
import com.lyy.keepassa.dao.AppDatabase;
import com.lyy.keepassa.entity.DbHistoryRecord;
import com.lyy.keepassa.receiver.ScreenLockReceiver;
import com.lyy.keepassa.router.ServiceRouter;
import com.lyy.keepassa.service.feat.KpaSdkService;
import com.lyy.keepassa.util.CommonKVStorage;
import com.lyy.keepassa.util.LanguageUtil;
import com.lyy.keepassa.view.StorageType;
import java.util.Locale;
import me.weishu.reflection.Reflection;

public class BaseApp extends MultiDexApplication {

  public static BaseApp APP;
  public static Handler handler;
  public static Database KDB;
  @Nullable
  public static DbHistoryRecord dbRecord;
  public static AppDatabase appDatabase;
  public static String dbName = "";
  public static String dbFileName = "";
  public static String dbVersion = "Keepass 4.0";
  // SHA 256 加密后的数据库主密码
  public static String dbPass = "";
  public static String shortPass = "";
  // SHA 256 加密后的密钥路径
  public static String dbKeyPath = "";
  public static boolean isV4 = true;
  public static Locale currentLang = Locale.ENGLISH;
  public static Boolean isLocked = true;

  public static int passType = PassType.INSTANCE.getONLY_PASS();

  public static boolean isAFS() {
    return dbRecord == null || StorageType.valueOf(dbRecord.getType()) == StorageType.AFS;
  }

  @Override
  protected void attachBaseContext(Context base) {
    currentLang = setLanguage(base);
    //super.attachBaseContext(LanguageUtil.INSTANCE.setLanguage(base, currentLang));
    super.attachBaseContext(base);
    setThemeStyle();
    Reflection.unseal(base);
  }

  private void setThemeStyle() {
    String mode = PreferenceManager.getDefaultSharedPreferences(this)
        .getString(getString(R.string.set_key_theme_style), "0");
    switch (mode) {
      case "0":
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        break;
      case "1":
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        break;
      case "2":
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        break;
    }
  }

  @Override public void onCreate() {
    super.onCreate();
    AbsFrame.init(this);
    APP = this;
    handler = new Handler(Looper.getMainLooper());
    ARouter.init(this); // 尽可能早，推荐在Application中初始化
    KpaSdkService kpaSdkService = Routerfit.INSTANCE.create(ServiceRouter.class).getKpaSdkService();
    kpaSdkService.preInitSdk(this);
    initReceiver();
    if (CommonKVStorage.INSTANCE.getBoolean(Constance.IS_AGREE_PRIVACY_AGREEMENT, false)) {
      kpaSdkService.initThirdSdk(this);
    }
  }

  /**
   * init receiver
   */
  public void initReceiver() {
    boolean isNeedRegScreenLockReceiver = PreferenceManager.getDefaultSharedPreferences(BaseApp.APP)
        .getBoolean(getString(R.string.set_key_lock_screen_auto_lock_db), false);
    if (isNeedRegScreenLockReceiver) {
      ScreenLockReceiver receiver = new ScreenLockReceiver();
      IntentFilter inf = new IntentFilter();
      inf.addAction(Intent.ACTION_SCREEN_OFF);
      inf.addAction(Intent.ACTION_USER_PRESENT);
      registerReceiver(receiver, inf);
    }
  }

  /**
   * 设置语言
   * 优先读取保存的语言，如果配置的语言存在，设置该语言为app的语言
   * 如果没有已记录的语言，读取系统当前语言
   * 如果系统语言不在支持列表的[LanguageUtil.SUPPORT_LAN]中，将app语言设置为英文
   * 如果系统语言在支持列表中，设置该语言为app的语言
   */
  private Locale setLanguage(Context context) {
    Locale lang = LanguageUtil.INSTANCE.getDefLanguage(context);
    if (lang != null) {
      currentLang = lang;
    } else {
      Locale def = LanguageUtil.INSTANCE.getSysCurrentLan();
      lang = new Locale(def.getLanguage(), def.getCountry());
      if (LanguageUtil.SUPPORT_LAN.contains(lang)) {
        LanguageUtil.INSTANCE.setLanguage(context, lang);
      } else {
        LanguageUtil.INSTANCE.setLanguage(context, Locale.ENGLISH);
      }
      LanguageUtil.INSTANCE.saveLanguage(context, lang);
    }
    return lang;
  }
}