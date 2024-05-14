/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.detail

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.view.ViewAnimationUtils
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.ScreenUtils
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedBinary
import com.keepassdroid.database.security.ProtectedString
import com.keepassdroid.utils.Types
import com.keepassdroid.utils.UriUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.anim.ColorEvaluator
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.databinding.ActivityEntryDetailNewBinding
import com.lyy.keepassa.entity.EntryRecord
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.widget.toPx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.max

/**
 * 条目详情
 */
class EntryDetailModule : BaseModule() {
  private lateinit var pwEntry: PwEntry

  fun initEntry(pwEntry: PwEntry) {
    this.pwEntry = pwEntry
  }

  fun finishRevealAnim(ac: EntryDetailActivityNew) {
    val binding = ac.binding
    val vAnim = AnimatorSet()
    val revealAnimal = ViewAnimationUtils.createCircularReveal(
      binding.root,
      ScreenUtils.getScreenWidth(),
      ScreenUtils.getScreenHeight(),
      max(ScreenUtils.getScreenWidth().toFloat(), ScreenUtils.getScreenHeight().toFloat()),
      0.toFloat()
    )
    val contentAnim1 = ObjectAnimator.ofFloat(binding.topAppBar, View.ALPHA, 1f, 0f)
    val contentAnim2 = ObjectAnimator.ofFloat(binding.clContentRoot, View.ALPHA, 1f, 0f)

    vAnim.duration = 400
    vAnim.doOnEnd {
      binding.groupContent.isGone = true
      binding.ivBlur.isGone = true
      ac.superFinish()
    }
    vAnim.playTogether(revealAnimal, contentAnim1, contentAnim2)
    vAnim.interpolator = revealAnimal.interpolator
    vAnim.start()
  }

  fun startRevealAnim(binding: ActivityEntryDetailNewBinding, resource: Drawable?) {
    val vAnim = AnimatorSet()
    val revealAnimal = ViewAnimationUtils.createCircularReveal(
      binding.root,
      ScreenUtils.getScreenWidth(),
      ScreenUtils.getScreenHeight(),
      0.toFloat(),
      max(ScreenUtils.getScreenWidth().toFloat(), ScreenUtils.getScreenHeight().toFloat())
    )

    val contentAnim1 = ObjectAnimator.ofFloat(binding.topAppBar, View.ALPHA, 0f, 1f)
    val contentAnim2 = ObjectAnimator.ofFloat(binding.clContentRoot, View.ALPHA, 0f, 1f)

    vAnim.duration = 400
    vAnim.doOnStart {
      binding.topAppBar.alpha = 0f
      binding.clContentRoot.alpha = 0f
      binding.groupContent.isVisible = true
      binding.ivBlur.setImageDrawable(resource)
    }
    vAnim.playTogether(revealAnimal, contentAnim1, contentAnim2)
    vAnim.interpolator = revealAnimal.interpolator
    vAnim.start()
  }

  /**
   * get highlight color
   */
  fun getColor(
    context: Context,
    icon: Drawable
  ): Pair<Int, Int> {
    return with(Dispatchers.IO) {
      val temp =
        IconUtil.getBitmapFromDrawable(context, icon, 40.toPx())
      if (temp == null || temp.isRecycled) {
        return@with Pair(Color.WHITE, ResUtil.getColor(R.color.color_444E85DB))
      }
      val sw = Palette.from(temp)
        .maximumColorCount(16)
        .generate()

      val iconColor = when {
        sw.mutedSwatch != null -> sw.mutedSwatch!!.rgb
        sw.darkMutedSwatch != null -> sw.darkMutedSwatch!!.rgb
        sw.lightMutedSwatch != null -> sw.lightMutedSwatch!!.rgb
        sw.darkVibrantSwatch != null -> sw.darkVibrantSwatch!!.rgb
        sw.lightVibrantSwatch != null -> sw.lightVibrantSwatch!!.rgb
        sw.vibrantSwatch != null -> sw.vibrantSwatch!!.rgb
        else -> ResUtil.getColor(R.color.color_444E85DB)
      }

      val bgColor =
        if (KeepassAUtil.instance.isNightMode()) sw.getDarkMutedColor(iconColor) else sw.getLightMutedColor(
          iconColor
        )

      return@with Pair(iconColor, bgColor)
    }
  }

  /**
   * 保存附件到sd卡
   * @param saveUri 保存路径
   * @param source 需要保存的文件
   */
  fun saveAttachment(
    context: Context,
    saveUri: Uri,
    source: ProtectedBinary
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val byte = source.data.readBytes()
        val os = context.contentResolver.openOutputStream(saveUri)
        if (os != null) {
          os.write(byte, 0, byte.size)
          os.flush()
          os.close()
        }
        withContext(Dispatchers.Main) {
          val fileName = UriUtil.getFileNameFromUri(context, saveUri)
          HitUtil.toaskShort(context.getString(R.string.save_file_success, fileName))
        }
      } catch (e: Exception) {
        Timber.e(e)
      }
    }
  }

  /**
   * 保存打开记录
   */
  fun saveRecord() {
    if (BaseApp.dbRecord == null) {
      return
    }
    KpaUtil.scope.launch(Dispatchers.IO) {
      val dao = BaseApp.appDatabase.entryRecordDao()
      var record = dao.getRecord(Types.UUIDtoBytes(pwEntry.uuid), BaseApp.dbRecord!!.localDbUri)
      if (record == null) {
        record = EntryRecord(
          userName = pwEntry.username,
          title = pwEntry.title,
          uuid = Types.UUIDtoBytes(pwEntry.uuid),
          time = System.currentTimeMillis(),
          dbFileUri = BaseApp.dbRecord!!.localDbUri
        )
        dao.saveRecord(record)
      } else {
        record.title = pwEntry.title
        record.userName = pwEntry.username
        record.time = System.currentTimeMillis()
        dao.updateRecord(record)
      }
      KpaUtil.openEntryRecordFlow.emit(record)
    }
  }

  /**
   * 获取项目的属性字段，只有v4版本才有自定义属性字段
   */
  fun getV4EntryStr(entryV4: PwEntryV4): Map<String, ProtectedString> {
    return KdbUtil.filterCustomStr(entryV4)
  }
}