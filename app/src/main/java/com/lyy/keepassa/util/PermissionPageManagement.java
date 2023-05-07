package com.lyy.keepassa.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import com.blankj.utilcode.util.RomUtils;
import timber.log.Timber;

public class PermissionPageManagement {

  /**
   * 此函数可以自己定义
   */
  public static void goToSetting(Context activity) {
    if (RomUtils.isHuawei()) {
      Huawei(activity);
      return;
    }

    if (RomUtils.isMeizu()) {
      Meizu(activity);
      return;
    }

    if (RomUtils.isXiaomi()) {
      Xiaomi(activity);
      return;
    }

    if (RomUtils.isVivo()) {
      VIVO(activity);
      return;
    }

    if (RomUtils.isOppo()) {
      OPPO(activity);
      return;
    }
    if (RomUtils.isSony()) {
      Sony(activity);
      return;
    }
    if (RomUtils.isLg()) {
      LG(activity);
      return;
    }
    ApplicationInfo(activity);
    Timber.e("目前暂不支持此系统");
  }

  public static void Huawei(Context activity) {
    try {
      Intent intent = new Intent();
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.putExtra("packageName", activity.getApplicationInfo().packageName);
      ComponentName comp = new ComponentName("com.huawei.systemmanager",
          "com.huawei.permissionmanager.ui.MainActivity");
      intent.setComponent(comp);
      activity.startActivity(intent);
    } catch (Exception e) {
      Timber.e(e);
      goIntentSetting(activity);
    }
  }

  public static void Meizu(Context activity) {
    try {
      Intent intent = new Intent("com.meizu.safe.security.SHOW_APPSEC");
      intent.addCategory(Intent.CATEGORY_DEFAULT);
      intent.putExtra("packageName", activity.getPackageName());
      activity.startActivity(intent);
    } catch (Exception e) {
      goIntentSetting(activity);
      Timber.e(e);
    }
  }

  public static void Xiaomi(Context activity) {
    try {
      Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
      intent.putExtra("extra_pkgname", activity.getPackageName());

      //ComponentName componentName = new ComponentName("com.miui.securitycenter",
      //    "com.miui.permcenter.permissions.PermissionsEditorActivity");
      //intent.setComponent(componentName);
      intent.addCategory(Intent.CATEGORY_DEFAULT);
      activity.startActivity(intent);
    } catch (Exception e) {
      goIntentSetting(activity);
      Timber.e(e);
    }
  }

  public static void Sony(Context activity) {
    try {
      Intent intent = new Intent();
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.putExtra("packageName", activity.getPackageName());
      ComponentName comp =
          new ComponentName("com.sonymobile.cta", "com.sonymobile.cta.SomcCTAMainActivity");
      intent.setComponent(comp);
      activity.startActivity(intent);
    } catch (Exception e) {
      goIntentSetting(activity);
      Timber.e(e);
    }
  }

  public static void OPPO(Context activity) {
    try {
      Intent intent = new Intent();
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.putExtra("packageName", activity.getPackageName());
      //        ComponentName comp = new ComponentName("com.color.safecenter", "com.color.safecenter.permission.PermissionManagerActivity");
      ComponentName comp = new ComponentName("com.coloros.securitypermission",
          "com.coloros.securitypermission.permission.PermissionAppAllPermissionActivity");//R11t 7.1.1 os-v3.2
      intent.setComponent(comp);
      activity.startActivity(intent);
    } catch (Exception e) {
      goIntentSetting(activity);
      Timber.e(e);
    }
  }

  public static void VIVO(Context activity) {
    Intent localIntent;
    if (((Build.MODEL.contains("Y85")) && (!Build.MODEL.contains("Y85A"))) || (Build.MODEL.contains(
        "vivo Y53L"))) {
      localIntent = new Intent();
      localIntent.setClassName("com.vivo.permissionmanager",
          "com.vivo.permissionmanager.activity.PurviewTabActivity");
      localIntent.putExtra("packagename", activity.getPackageName());
      localIntent.putExtra("tabId", "1");
      activity.startActivity(localIntent);
    } else {
      localIntent = new Intent();
      localIntent.setClassName("com.vivo.permissionmanager",
          "com.vivo.permissionmanager.activity.SoftPermissionDetailActivity");
      localIntent.setAction("secure.intent.action.softPermissionDetail");
      localIntent.putExtra("packagename", activity.getPackageName());
      activity.startActivity(localIntent);
    }
  }

  public static void LG(Context activity) {
    try {
      Intent intent = new Intent("android.intent.action.MAIN");
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.putExtra("packageName", activity.getPackageName());
      ComponentName comp = new ComponentName("com.android.settings",
          "com.android.settings.Settings$AccessLockSummaryActivity");
      intent.setComponent(comp);
      activity.startActivity(intent);
    } catch (Exception e) {
      goIntentSetting(activity);
      Timber.e(e);
    }
  }

  /**
   * 只能打开到自带安全软件
   */
  public static void _360(Context activity) {
    Intent intent = new Intent("android.intent.action.MAIN");
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.putExtra("packageName", activity.getPackageName());
    ComponentName comp = new ComponentName("com.qihoo360.mobilesafe",
        "com.qihoo360.mobilesafe.ui.index.AppEnterActivity");
    intent.setComponent(comp);
    activity.startActivity(intent);
  }

  /**
   * 应用信息界面
   */
  public static void ApplicationInfo(Context activity) {
    Intent localIntent = new Intent();
    localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
    localIntent.setData(Uri.fromParts("package", activity.getPackageName(), null));
    activity.startActivity(localIntent);
  }

  /**
   * 系统设置界面
   */
  public static void SystemConfig(Context activity) {
    Intent intent = new Intent(Settings.ACTION_SETTINGS);
    activity.startActivity(intent);
  }

  /**
   * 默认打开应用详细页
   */
  private static void goIntentSetting(Context pActivity) {
    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    Uri uri = Uri.fromParts("package", pActivity.getPackageName(), null);
    intent.setData(uri);
    try {
      pActivity.startActivity(intent);
    } catch (Exception e) {
      Timber.e(e);
    }
  }
}
