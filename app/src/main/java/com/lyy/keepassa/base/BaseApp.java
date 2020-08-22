/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.base;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.multidex.MultiDexApplication;
import androidx.room.Room;
import com.arialyy.frame.core.AbsFrame;
import com.arialyy.frame.util.KeyStoreUtil;
import com.keepassdroid.Database;
import com.leon.channel.helper.ChannelReaderUtil;
import com.lyy.keepassa.BuildConfig;
import com.lyy.keepassa.baseapi.INotFreeLibService;
import com.lyy.keepassa.dao.AppDatabase;
import com.lyy.keepassa.entity.DbRecord;
import com.lyy.keepassa.nofreelib.BuglyConfig;
import com.lyy.keepassa.util.AutoLockDbUtil;
import com.lyy.keepassa.util.KeepassAUtil;
import com.lyy.keepassa.util.LanguageUtil;
import com.lyy.keepassa.util.QuickUnLockUtil;
import com.lyy.keepassa.view.DbPathType;
import com.tencent.wcdb.database.SQLiteCipherSpec;
import com.tencent.wcdb.room.db.WCDBOpenHelperFactory;
import com.zzhoujay.richtext.RichText;
import java.util.Iterator;
import java.util.Locale;
import java.util.ServiceLoader;

public class BaseApp extends MultiDexApplication {

  public static BaseApp APP;
  public static Handler handler;
  public static Database KDB;
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
  public static DbRecord dbRecord;
  public static Boolean isLocked = true;

  public static boolean isAFS() {
    return dbRecord == null || DbPathType.valueOf(dbRecord.getType()) == DbPathType.AFS;
  }

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
  }

  @Override public void onCreate() {
    super.onCreate();
    AbsFrame.init(this);
    APP = this;
    handler = new Handler(Looper.getMainLooper());
    // 初始化一下时间
    KeepassAUtil.INSTANCE.isFastClick();
    // 进入应用时移除锁定数据库的定时器
    AutoLockDbUtil.Companion.get().cancelTimer();
    currentLang = setLanguage();
    initDb();

    // 开启kotlin 协程debug
    if (BuildConfig.DEBUG) {
      System.setProperty("kotlinx.coroutines.debug", "on");
    } else {
      if (BuildConfig.FLAVOR != "fdroid"){
        initNotFreeLib();
      }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      KeyStoreUtil.Companion.setKeyStorePass(QuickUnLockUtil.getDbPass().toCharArray());
    }
    RichText.initCacheDir(this);
  }

  public String getChannel() {
    String channel = ChannelReaderUtil.getChannel(getApplicationContext());
    if (channel == null) {
      channel = "default";
    }
    return channel;
  }

  /**
   * 使用spi机制初始化自由软件
   */
  private void initNotFreeLib() {
    ServiceLoader<INotFreeLibService> loader = ServiceLoader.load(INotFreeLibService.class);
    Iterator<INotFreeLibService> iterator = loader.iterator();
    while (iterator.hasNext()) {
      iterator.next().initLib(this, BuildConfig.DEBUG, getChannel(), null);
    }
  }

  /**
   * 初始化数据库
   */
  private void initDb() {
    // 初始化数据库
    SQLiteCipherSpec cipherSpec = new SQLiteCipherSpec()  // 指定加密方式，使用默认加密可以省略
        .setPageSize(4096)
        .setKDFIteration(64000);

    WCDBOpenHelperFactory factory = new WCDBOpenHelperFactory()
        .passphrase(QuickUnLockUtil.getDbPass().getBytes())  // 指定加密DB密钥，非加密DB去掉此行
        .cipherSpec(cipherSpec)               // 指定加密方式，使用默认加密可以省略
        .writeAheadLoggingEnabled(true)       // 打开WAL以及读写并发，可以省略让Room决定是否要打开
        .asyncCheckpointEnabled(true);        // 打开异步Checkpoint优化，不需要可以省略

    appDatabase = Room.databaseBuilder(this, AppDatabase.class, AppDatabase.DB_NAME)
        .openHelperFactory(factory)
        .addMigrations(DbMigration.INSTANCE.MIGRATION_2_3())
        .build();
  }

  /**
   * 设置语言
   * 优先读取保存的语言，如果配置的语言存在，设置该语言为app的语言
   * 如果没有已记录的语言，读取系统当前语言
   * 如果系统语言不在支持列表的[LanguageUtil.SUPPORT_LAN]中，将app语言设置为英文
   * 如果系统语言在支持列表中，设置该语言为app的语言
   */
  private Locale setLanguage() {
    Locale lang = LanguageUtil.INSTANCE.getDefLanguage(this);
    if (lang != null) {
      currentLang = lang;
    } else {
      Locale def = LanguageUtil.INSTANCE.getSysCurrentLan();
      lang = new Locale(def.getLanguage(), def.getCountry());
      if (LanguageUtil.SUPPORT_LAN.contains(lang)) {
        LanguageUtil.INSTANCE.setLanguage(this, lang);
      } else {
        LanguageUtil.INSTANCE.setLanguage(this, Locale.ENGLISH);
      }
    }
    return lang;
  }
}