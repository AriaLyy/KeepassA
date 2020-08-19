/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


packageManager = context.packageManager
      return packageManager.getApplicationLabel(
          packageManager.getApplicationInfo(
              apkPkgName,
              PackageManager.GET_META_DATA
          )
      ).toString()
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return null
  }

  /**
   * 获取图标 bitmap
   * @param context
   */
  fun getAppIcon(
    context: Context,
    apkPkgName: String
  ): Bitmap? {
    val d = context.packageManager.getApplicationIcon(apkPkgName) ?: return null
    return IconUtil.getBitmapFromDrawable(context, d)
  }

}