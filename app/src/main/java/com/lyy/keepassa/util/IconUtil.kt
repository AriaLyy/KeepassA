/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.bumptech.glide.Glide
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV3
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupV3
import com.keepassdroid.database.PwGroupV4
import com.keepassdroid.database.PwIconCustom
import com.lyy.keepassa.R
import com.lyy.keepassa.R.dimen

object IconUtil {

  val icons = listOf(
      R.drawable.cc0_password, R.drawable.cc1_package_network, R.drawable.cc2_messagebox_warning,
      R.drawable.cc3_server, R.drawable.cc4_klipper, R.drawable.cc5_edu_languages,
      R.drawable.cc6_kcmdf, R.drawable.cc7_kate, R.drawable.cc8_socket, R.drawable.cc9_identity,
      R.drawable.cc10_kontact, R.drawable.cc11_camera, R.drawable.cc12_irkick_flash,
      R.drawable.cc13_kgpg_key3, R.drawable.cc14_laptop_power, R.drawable.cc15_scanner,
      R.drawable.cc16_mozilla_firebird, R.drawable.cc17_cdrom_unmount, R.drawable.cc18_display,
      R.drawable.cc19_email_generic, R.drawable.cc20_misc, R.drawable.cc21_korganizer,
      R.drawable.cc22_ascii, R.drawable.cc23_icons, R.drawable.cc24_connect_established,
      R.drawable.cc25_folder_mail, R.drawable.cc26_file_save, R.drawable.cc27_nfs_unmount,
      R.drawable.cc28_quick_time, R.drawable.cc29_kgpg_term, R.drawable.cc30_konsole,
      R.drawable.cc31_file_print, R.drawable.cc32_fsview, R.drawable.cc33_run,
      R.drawable.cc34_configure, R.drawable.cc35_krfb, R.drawable.cc36_ark,
      R.drawable.cc37_kpercentage, R.drawable.cc38_samba_unmount, R.drawable.cc39_history,
      R.drawable.cc40_mail_find, R.drawable.cc41_vectorgfx, R.drawable.cc42_kcmemory,
      R.drawable.cc43_edit_trash, R.drawable.cc44_knotes, R.drawable.cc45_cancel,
      R.drawable.cc46_help, R.drawable.cc47_kpackage, R.drawable.cc48_folder,
      R.drawable.cc49_folder_blue_open, R.drawable.cc50_folder_tar, R.drawable.cc51_decrypted,
      R.drawable.cc52_encrypted, R.drawable.cc53_apply, R.drawable.cc54_signature,
      R.drawable.cc55_thumbnail, R.drawable.cc56_kaddress_book, R.drawable.cc57_view_text,
      R.drawable.cc58_kgpg, R.drawable.cc59_package_development, R.drawable.cc60_kfm_home,
      R.drawable.cc61_services, R.drawable.cc62_tux, R.drawable.cc63_feather,
      R.drawable.cc64_apple, R.drawable.cc65_w, R.drawable.cc66_money,
      R.drawable.cc67_certificate, R.drawable.cc68_blackberry
  )

  fun getIconById(kpaIconId: Int): Int {
    return icons[kpaIconId]
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
    return getBitmapFromDrawable(context, d)
  }

  /**
   * 将自定义图标转换为drawable，如果自定义图标为空，则需要返回默认图标
   * @param defIcon 默认图标
   */
  fun convertCustomIcon2Drawable(
    context: Context,
    customIcon: PwIconCustom,
    @DrawableRes defIcon: Int = R.drawable.ic_image_blue_24px
  ): Drawable {
    if (customIconIsNull(customIcon)) {
      return context.resources.getDrawable(defIcon)
    }
    val bd = BitmapDrawable(
        context.resources,
        BitmapFactory.decodeByteArray(
            customIcon.imageData,
            0,
            customIcon.imageData.size
        )
    )
    return zoomDrawable(context, bd)
  }

  /**
   * 设置组的icon
   */
  fun setGroupIcon(
    context: Context,
    group: PwGroup,
    img: ImageView
  ) {
    if (group is PwGroupV3) {
      Glide.with(context)
          .load(getIconById(group.icon.iconId))
          .into(img)
      return
    }
    if (customIconIsNull((group as PwGroupV4).customIcon)) {
      Glide.with(context)
          .load(getIconById(group.icon.iconId))
          .into(img)
      return
    }
    Glide.with(context)
        .load(group.customIcon.imageData)
        .into(img)
  }

  /**
   * 获取group的drawable
   */
  fun getGroupIconDrawable(
    context: Context,
    group: PwGroup,
    zoomIcon: Boolean = false
  ): Drawable? {
    if (group is PwGroupV3) {
      return context.getDrawable(getIconById(group.icon.iconId))
    } else {
      val v4Group = group as PwGroupV4
      return if (!customIconIsNull(group.customIcon)) {
        val dr = BitmapDrawable(
            context.resources,
            BitmapFactory.decodeByteArray(
                group.customIcon.imageData, 0,
                group.customIcon.imageData.size
            )
        )
        if (zoomIcon) zoomDrawable(context, dr) else dr
      } else {
        context.getDrawable(getIconById(group.icon.iconId))
      }
    }
  }

  /**
   * 获取entry的drawable
   */
  fun getEntryIconDrawable(
    context: Context,
    entry: PwEntry,
    zoomIcon: Boolean = false
  ): Drawable? {
    if (entry is PwEntryV3) {
      return context.getDrawable(getIconById(entry.icon.iconId))
    } else {
      val v4Entry = entry as PwEntryV4
      return if (!customIconIsNull(entry.customIcon)) {
        val dr = BitmapDrawable(
            context.resources,
            BitmapFactory.decodeByteArray(
                entry.customIcon.imageData, 0,
                entry.customIcon.imageData.size
            )
        )
        if (zoomIcon) zoomDrawable(context, dr) else dr
      } else {
        context.getDrawable(getIconById(entry.icon.iconId))
      }
    }
  }

  /**
   * 设置项目的icon
   */
  fun setEntryIcon(
    context: Context,
    entry: PwEntry,
    icon: ImageView
  ) {
    if (entry.icon == null) {
      return
    }
    if (entry is PwEntryV3) {
      Glide.with(context)
          .load(getIconById(entry.icon.iconId))
          .into(icon)
      return
    }
    if (entry is PwEntryV4) {
      if (!customIconIsNull(entry.customIcon)) {
        Glide.with(context)
            .load(entry.customIcon.imageData)
            .error(R.drawable.ic_image_broken_24px)
            .into(icon)
        return
      }
      Glide.with(context)
          .load(getIconById(entry.icon.iconId))
          .into(icon)
    }
  }

  /**
   * 检查自定义图标是否为空
   * @return true 自定义图标为空
   */
  private fun customIconIsNull(customIcon: PwIconCustom?): Boolean {
    return (customIcon?.imageData == null) || customIcon.imageData.isEmpty() || customIcon == PwIconCustom.ZERO
  }

  /**
   * 调整drawable大小
   */
  fun zoomDrawable(
    context: Context,
    drawable: BitmapDrawable
  ): BitmapDrawable {
    val iconSize = context.resources.getDimension(R.dimen.icon_size)

    val newbmp =
      Bitmap.createScaledBitmap(drawable.bitmap, iconSize.toInt(), iconSize.toInt(), true)
    drawable.bitmap.recycle()
    return BitmapDrawable(context.resources, newbmp)
  }

  /**
   * drawable 转bitmap
   * @param iconSize 默认28dp，如果设置-1，则为图片本身大小
   */
  fun getBitmapFromDrawable(
    context: Context,
    @DrawableRes drawableId: Int,
    iconSize: Int = context.resources.getDimension(dimen.icon_size)
        .toInt()
  ): Bitmap? {
    val drawable: Drawable? = context.getDrawable(drawableId)
    return getBitmapFromDrawable(context, drawable, iconSize)
  }

  fun getBitmapFromDrawable(
    context: Context,
    drawable: Drawable?,
    iconSize: Int = context.resources.getDimension(dimen.icon_size)
        .toInt()
  ): Bitmap? {
    return if (drawable is BitmapDrawable) {
      drawable.bitmap
    } else if (drawable is VectorDrawable || drawable is VectorDrawableCompat || drawable is LayerDrawable) {
      val bitmap = Bitmap.createBitmap(
          drawable.intrinsicWidth,
          drawable.intrinsicHeight,
          ARGB_8888
      )
      val canvas = Canvas(bitmap)
      if (iconSize == -1) {
        drawable.setBounds(0, 0, canvas.width, canvas.height)
      } else {
        drawable.setBounds(0, 0, iconSize, iconSize)
      }
      drawable.draw(canvas)
      bitmap
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
      val drr = arrayOfNulls<Drawable>(2)
      drr[0] = drawable.background
      drr[1] = drawable.foreground
      val layerDrawable = LayerDrawable(drr)
      return getBitmapFromDrawable(context, layerDrawable, layerDrawable.intrinsicWidth)
    } else {
      throw IllegalArgumentException("unsupported drawable type")
    }
  }

}