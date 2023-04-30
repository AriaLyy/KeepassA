/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.detail

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.InputType
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.arialyy.frame.module.SingleLiveEvent
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedBinary
import com.keepassdroid.database.security.ProtectedString
import com.keepassdroid.utils.Types
import com.keepassdroid.utils.UriUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.R.color
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.entity.EntryRecord
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.VibratorUtil
import com.lyy.keepassa.view.menu.EntryDetailFilePopMenu
import com.lyy.keepassa.view.menu.EntryDetailStrPopMenu
import com.lyy.keepassa.view.menu.EntryDetailStrPopMenu.OnShowPassCallback
import com.lyy.keepassa.widget.expand.AttrFileItemView
import com.lyy.keepassa.widget.expand.AttrStrItemView
import com.lyy.keepassa.widget.toPx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

/**
 * 条目详情
 */
class EntryDetailModule : BaseModule() {
  var curDLoadFile: ProtectedBinary? = null
  var lastCollection: Boolean = false
  private lateinit var pwEntry: PwEntry
  private val finishAnimEvent = SingleLiveEvent<Boolean>()
  private val startAnimEvent = SingleLiveEvent<Boolean>()

  override fun onCleared() {
    super.onCleared()
  }

  fun initEntry(pwEntry: PwEntry) {
    this.pwEntry = pwEntry
  }

  /**
   * 结束动画
   */
  fun finishAnim(
    context: Context,
    rootView: View,
    icon: ImageView
  ): SingleLiveEvent<Boolean> {
    viewModelScope.launch {
      val rgb = getColor(context, icon.drawable)
      val x = icon.x + 20.toPx()
      val y = icon.y + 60.toPx()
      val anim = ViewAnimationUtils.createCircularReveal(
        rootView,
        x.toInt(),
        y.toInt(),
        rootView.height.toFloat(),
        0f,
      )
      anim.duration = 400
      anim.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationStart(animation: Animator) {
          super.onAnimationStart(animation)
          rootView.background = ColorDrawable(rgb)
        }

        override fun onAnimationEnd(animation: Animator) {
          super.onAnimationEnd(animation)
          rootView.background = ColorDrawable(ResUtil.getColor(R.color.background_color))
          finishAnimEvent.postValue(true)
        }
      })
      anim.start()
    }

    return finishAnimEvent
  }

  /**
   * 启动动画
   */
  fun startAnim(
    context: Context,
    rootView: View,
    icon: ImageView
  ): SingleLiveEvent<Boolean> {
    viewModelScope.launch {
      val rgb = getColor(context, icon.drawable)
      val x = icon.x + 20.toPx()
      val y = icon.y + 60.toPx()
      val anim = ViewAnimationUtils.createCircularReveal(
        rootView,
        x.toInt(),
        y.toInt(),
        40.toPx()
          .toFloat(),
        rootView.height.toFloat()
      )
      anim.duration = 400
      anim.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationStart(animation: Animator) {
          super.onAnimationStart(animation)
          rootView.background = ColorDrawable(rgb)
        }

        override fun onAnimationEnd(animation: Animator) {
          super.onAnimationEnd(animation)
          rootView.background = ColorDrawable(ResUtil.getColor(color.background_color))
          startAnimEvent.postValue(true)
        }
      })
      anim.start()
    }
    return startAnimEvent
  }

  /**
   * get highlight color
   */
  private fun getColor(
    context: Context,
    icon: Drawable
  ): Int {
    return with(Dispatchers.IO) {
      val temp =
        IconUtil.getBitmapFromDrawable(context, icon, 40.toPx())
      if (temp == null || temp.isRecycled) {
        return@with Color.WHITE
      }
      val sw = Palette.from(temp)
        .maximumColorCount(12)
        .generate()
      return@with when {
        sw.mutedSwatch != null -> sw.mutedSwatch!!.rgb
        sw.darkMutedSwatch != null -> sw.darkMutedSwatch!!.rgb
        sw.lightMutedSwatch != null -> sw.lightMutedSwatch!!.rgb
        sw.darkVibrantSwatch != null -> sw.darkVibrantSwatch!!.rgb
        sw.lightVibrantSwatch != null -> sw.lightVibrantSwatch!!.rgb
        sw.vibrantSwatch != null -> sw.vibrantSwatch!!.rgb
        else -> ResUtil.getColor(R.color.colorPrimary)
      }
    }
  }

  /**
   * 展示附件的菜单
   */
  fun showAttrFilePopMenu(
    context: EntryDetailActivity,
    v: View
  ) {
    if (KeepassAUtil.instance.isFastClick()) {
      return
    }
    val key = (v as AttrFileItemView).titleStr
    val value = v.file
    val menu = EntryDetailFilePopMenu(context, v, key, value!!)
    menu.setOnDownloadClick(object : EntryDetailFilePopMenu.OnDownloadClick {
      override fun onDownload(
        key: String,
        file: ProtectedBinary
      ) {
        curDLoadFile = file
        context.saveAttachmentResult.launch("*/*")
      }
    })
    menu.show()
  }

  /**
   * 展示属性字段的菜单
   */
  fun showAttrStrPopMenu(
    context: FragmentActivity,
    v: View
  ) {
    if (KeepassAUtil.instance.isFastClick()) {
      return
    }
    val value = v.findViewById<TextView>(R.id.value)
    val key = (v as AttrStrItemView).titleStr
    val str = v.valueInfo
    val pop = EntryDetailStrPopMenu(context, v, str)
    // totp 密码，seed都需要显示密码
    if (key == "TOTP"
      || key.equals("otp", ignoreCase = true)
      || key.equals("TOTP Seed", ignoreCase = true)
      || str.isProtected
    ) {
      pop.setOnShowPassCallback(object : OnShowPassCallback {
        override fun showPass(showPass: Boolean) {
          if (showPass) {
            value.inputType =
              (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
            return
          }
          value.inputType =
            (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        }
      })

      if (value.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)) {
        pop.setHidePass()
      }
    }

    pop.show()
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
        e.printStackTrace()
      }
    }
  }

  /**
   * 回收项目
   * @param pwEntry 需要回收的条目
   */
  fun recycleEntry(ac: FragmentActivity, pwEntry: PwEntryV4) {
    KpaUtil.kdbHandlerService.deleteEntry(pwEntry){
      HitUtil.toaskShort(
        "${ac.getString(R.string.del_entry)}${ac.getString(R.string.success)}"
      )
      VibratorUtil.vibrator(300)
      ac.finishAfterTransition()
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
      EventBus.getDefault()
        .post(record)
    }
  }

  /**
   * 获取项目的属性字段，只有v4版本才有自定义属性字段
   */
  fun getV4EntryStr(entryV4: PwEntryV4): Map<String, ProtectedString> {
    return KeepassAUtil.instance.filterCustomStr(entryV4)
  }
}