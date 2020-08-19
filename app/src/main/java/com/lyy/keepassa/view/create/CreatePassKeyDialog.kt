/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.create

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.arialyy.frame.util.StringUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.keepassdroid.utils.UriUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseBottomSheetDialogFragment
import com.lyy.keepassa.databinding.DialogPassKeyBinding
import com.lyy.keepassa.event.KeyPathEvent
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.KeepassAUtil.takePermission
import com.lyy.keepassa.util.PasswordBuilUtil
import org.greenrobot.eventbus.EventBus
import java.io.FileOutputStream
import java.io.IOException

/**
 * 创建key对话框
 */
class CreatePassKeyDialog : BaseBottomSheetDialogFragment<DialogPassKeyBinding>(),
    View.OnClickListener {
  private lateinit var behavior: BottomSheetBehavior<*>
  private val openFileReqCode = 0xB1
  private val createFileReqCode = 0xB2
  override fun setLayoutId(): Int {
    return R.layout.dialog_pass_key
  }

  override fun init(savedInstanceState: Bundle?) {
    super.init(savedInstanceState)
    behavior = BottomSheetBehavior.from(binding.content)
    binding.close.setOnClickListener(this)
    binding.item1.setOnClickListener(this)
    binding.item2.setOnClickListener(this)
    behavior.state = BottomSheetBehavior.STATE_EXPANDED
  }

  override fun onClick(v: View?) {
    when (v!!.id) {
      R.id.close -> dismiss()
      R.id.item_1 -> {
        KeepassAUtil.openSysFileManager(this, "*/*", openFileReqCode)
      }
      R.id.item_2 -> {
        KeepassAUtil.createFile(
            this, "*/*", "${getString(R.string.app_name)}.passkey", createFileReqCode
        )
      }
    }
  }

  /**
   * 将一个随机字符串写入密钥文件中
   */
  private fun writeData(uri: Uri?) {
    val fos = requireContext().contentResolver.openOutputStream(uri!!) as FileOutputStream
    try {
      val str = PasswordBuilUtil.getInstance()
          .addLowerChar()
          .addNumChar()
          .addMinus()
          .addSymbolChar()
          .builder(128)
      fos.write(
          StringUtil.keyToHashKey(str)
              .toByteArray()
      )
      fos.flush()
    } catch (e: IOException) {
      e.printStackTrace()
    } finally {
      fos.close()
    }
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == Activity.RESULT_OK && data != null && data.data != null) {
      // 申请长期的uri权限
      data.data?.takePermission()
      when (requestCode) {
        openFileReqCode -> {
          EventBus.getDefault()
              .post(
                  KeyPathEvent(
                      keyName = UriUtil.getFileNameFromUri(requireContext(), data.data),
                      keyUri = data.data!!
                  )
              )
        }
        createFileReqCode -> {
          writeData(data.data)
          Toast.makeText(
              context, getString(
              R.string.create_pass_key_success,
              UriUtil.getFileNameFromUri(requireContext(), data.data!!)
          ), Toast.LENGTH_SHORT
          )
              .show()
          EventBus.getDefault()
              .post(
                  KeyPathEvent(
                      keyName = UriUtil.getFileNameFromUri(requireContext(), data.data),
                      keyUri = data.data!!
                  )
              )
        }
        else -> {
          Log.e(TAG, "为止请求码：$requestCode")
        }
      }
      dismiss()
    } else {
      HitUtil.toaskShort("${getString(R.string.invalid)} ${getString(R.string.key)}")
      Log.e(TAG, "选择密钥文件失败，data为空")
    }
  }
}