/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.menu

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.MenuInflater
import android.view.View
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentActivity
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.FileUtil
import com.arialyy.frame.util.ReflectionUtil
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.security.ProtectedBinary
import com.lyy.keepassa.R
import com.lyy.keepassa.router.DialogRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.channels.Channels

/**
 * 项目详情附件悬浮菜单
 * @param x 偏移量
 */
@SuppressLint("RestrictedApi")
class EntryDetailFilePopMenu(
  private val context: FragmentActivity,
  view: View,
  private val key: String,
  private val file: ProtectedBinary
) : IPopMenu {
  private val popup: PopupMenu = PopupMenu(context, view, Gravity.END)
  private val help: MenuPopupHelper
  private var onDownloadClick: OnDownloadClick? = null
  private val scope = MainScope()

  interface OnDownloadClick {
    fun onDownload(
      key: String,
      file: ProtectedBinary
    )
  }

  init {
    val inflater: MenuInflater = popup.menuInflater
    inflater.inflate(R.menu.entry_detail_file_summary, popup.menu)
    // 以下代码为强制显示icon
    val mPopup = ReflectionUtil.getField(PopupMenu::class.java, "mPopup")
    mPopup.isAccessible = true
    help = mPopup.get(popup) as MenuPopupHelper
    help.setForceShowIcon(true)
    popup.setOnMenuItemClickListener { item ->
      when (item.itemId) {
        R.id.download_file -> {
          onDownloadClick?.onDownload(key, file)
        }
        R.id.open_whit_text -> {
          Routerfit.create(DialogRouter::class.java).showMsgDialog(
            msgTitle = ResUtil.getString(R.string.txt_viewer),
            msgContent = String(file.data.readBytes()),
            showCancelBt = false
          )
        }
        R.id.open_whit_img -> {
          val bytes = file.data.readBytes()
          if (bytes.isNotEmpty()) {
            Routerfit.create(DialogRouter::class.java).showImgViewerDialog(bytes)
          }
        }
        R.id.open_whit_other -> {
          // 将文件保存到缓存中
          openFile()
        }
      }
      dismiss()

      true
    }
  }

  /**
   * 将附件保存到缓存中，并打开
   */
  private fun openFile() {
    MainScope().launch {
      val targetFile = File(context.cacheDir, key)
      withContext(Dispatchers.IO) {
        val fic = Channels.newChannel(file.data)
        val foc = FileOutputStream(targetFile).channel
        foc.transferFrom(fic, 0, Int.MAX_VALUE.toLong())
        fic.close()
        foc.close()
      }
      FileUtil.openFile(context, targetFile)
      scope.cancel()
    }
  }

  public fun setOnDownloadClick(onDownloadClick: OnDownloadClick) {
    this.onDownloadClick = onDownloadClick
  }

  public fun show() {
    popup.show()
  }

  public fun dismiss() {
    popup.dismiss()
  }

}