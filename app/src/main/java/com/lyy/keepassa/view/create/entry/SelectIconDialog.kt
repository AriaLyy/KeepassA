/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.create.entry

import android.graphics.Color
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ImageUtils
import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwIcon
import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogChooseIconBinding
import com.lyy.keepassa.entity.KpaIconType
import com.lyy.keepassa.util.doClick
import com.lyy.keepassa.util.takePermission
import com.lyy.keepassa.view.icon.IconBottomSheetDialog
import com.lyy.keepassa.view.icon.IconItemCallback
import com.ypx.imagepicker.ImagePicker
import com.ypx.imagepicker.bean.selectconfig.CropConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.FileInputStream
import java.util.UUID

/**
 * @Author laoyuyu
 * @Description
 * @Date 10:07 2024/5/16
 **/
internal class SelectIconDialog : BaseDialog<DialogChooseIconBinding>() {

  companion object {
    internal val iconResultFlow = MutableSharedFlow<Pair<KpaIconType, PwIcon>>(0)
    private const val MAX_IMG_SIZE = 256
  }

  private val pickMedia = registerForActivityResult(PickVisualMedia()) { uri ->
    if (uri != null) {
      uri.takePermission(false)
      Timber.d("Selected URI: $uri")
      handlePhoto(uri)
      return@registerForActivityResult
    }

    Timber.d("No media selected")
    dismiss()
  }

  private fun handlePhoto(uri: Uri) {
    lifecycleScope.launch(Dispatchers.IO) {
      requireActivity().contentResolver.openInputStream(uri).use {
        if (it == null) {
          Timber.e("read photo input stream fail")
          dismiss()
          return@use
        }
        val bm = ImageUtils.getBitmap(it)
        // Crop images larger than 256
        if (bm.width > MAX_IMG_SIZE || bm.height > MAX_IMG_SIZE) {
          withContext(Dispatchers.Main) {
            cropImage(uri)
          }
          return@use
        }
        // Use directly
        val custom = PwIconCustom(UUID.randomUUID(), it.readBytes())
        iconResultFlow.emit(Pair(KpaIconType.CUSTOM, custom))
        dismiss()
      }
    }
  }

  private fun saveCustomIcon(pwIconCustom: PwIconCustom) {
    (BaseApp.KDB.pm as? PwDatabaseV4)?.putCustomIcons(pwIconCustom)
  }

  private fun cropImage(uri: Uri) {
    val cropConfig = CropConfig()
    //设置剪裁框间距，单位px
    cropConfig.cropRectMargin = MAX_IMG_SIZE
    //是否保存到DCIM目录下，false时会生成在 data/files/imagepicker/ 目录下
    cropConfig.saveInDCIM(false)
    //是否圆形剪裁，圆形剪裁时，setCropRatio无效
    cropConfig.isCircle = true
    //设置剪裁模式，留白或充满  CropConfig.STYLE_GAP 或 CropConfig.STYLE_FILL
    cropConfig.cropStyle = CropConfig.STYLE_FILL
    //设置留白模式下生成的图片背景色，支持透明背景
    cropConfig.cropGapBackgroundColor = Color.TRANSPARENT
    //调用剪裁
    ImagePicker.crop(
      requireActivity(), WeChatPresenter(), cropConfig, uri.toString()
    ) {
      if (it.isEmpty()) {
        Timber.e("crop photo fail")
        dismiss()
        return@crop
      }
      val path = it[0].cropUrl
      Timber.d("get crop img, path: $path")

      lifecycleScope.launch(Dispatchers.IO) {
        FileInputStream(path).use { fis ->
          val custom = PwIconCustom(UUID.randomUUID(), fis.readBytes())
          iconResultFlow.emit(Pair(KpaIconType.CUSTOM, custom))
          saveCustomIcon(custom)
          dismiss()
        }
      }
    }
  }

  override fun setLayoutId(): Int {
    return R.layout.dialog_choose_icon
  }

  override fun initData() {
    super.initData()
    binding.tvLocal.doClick {
      showLocalIcon()
      dismiss()
    }

    binding.tvPhotos.doClick {
      pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
    }
    dialog?.setCanceledOnTouchOutside(true)
  }

  private fun showLocalIcon() {
    val iconDialog = IconBottomSheetDialog()
    iconDialog.setCallback(object : IconItemCallback {
      override fun onDefaultIcon(defIcon: PwIconStandard) {
        lifecycleScope.launch {
          iconResultFlow.emit(Pair(KpaIconType.DEFAULT, defIcon))
        }
      }

      override fun onCustomIcon(customIcon: PwIconCustom) {
        lifecycleScope.launch {
          iconResultFlow.emit(Pair(KpaIconType.CUSTOM, customIcon))
        }
      }
    })
    iconDialog.show(requireActivity().supportFragmentManager)
  }
}