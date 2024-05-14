/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.detail

import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.ColorUtils
import com.blankj.utilcode.util.ToastUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedBinary
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.databinding.ActivityEntryDetailNewBinding
import com.lyy.keepassa.router.ActivityRouter
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.copyPassword
import com.lyy.keepassa.util.copyTotp
import com.lyy.keepassa.util.copyUserName
import com.lyy.keepassa.util.doClick
import com.lyy.keepassa.util.hasTOTP
import com.lyy.keepassa.util.isCollectioned
import com.lyy.keepassa.util.takePermission
import com.lyy.keepassa.view.detail.card.EntryFileCard
import com.lyy.keepassa.widget.toPx
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

/**
 * @Author laoyuyu
 * @Description
 * @Date 5:24 PM 2023/9/26
 **/
@Route(path = "/entry/detail")
class EntryDetailActivityNew : BaseActivity<ActivityEntryDetailNewBinding>() {
  companion object {
    const val KEY_ENTRY_ID = "KEY_ENTRY_ID"
  }

  private lateinit var module: EntryDetailModule
  private lateinit var pwEntry: PwEntryV4
  private var isInRecycleBin = false
  private var fileSaveCache: ProtectedBinary? = null

  private val saveFile = registerForActivityResult(CreateDocument("*/*")) {
    if (it == null) {
      Timber.e("uri为空")
      return@registerForActivityResult
    }
    if (fileSaveCache == null) {
      Timber.e("文件为空")
      return@registerForActivityResult
    }
    it.takePermission()
    lifecycleScope.launch(Dispatchers.IO) {
      contentResolver.openOutputStream(it).use { out ->
        val iChannel = Channels.newChannel(fileSaveCache!!.data)
        val oChannel = Channels.newChannel(out)
        val buffer = ByteBuffer.allocateDirect(16 * 1024)
        while (iChannel.read(buffer) != -1) {
          // 切换为读状态
          buffer.flip()
          // 保证缓冲区的数据全部写入
          while (buffer.hasRemaining()) {
            oChannel.write(buffer)
          }
          buffer.clear()
        }
        iChannel.close()
        oChannel.close()
        ToastUtils.showLong(ResUtil.getString(R.string.file_save_success))
      }
    }
  }

  @Autowired(name = KEY_ENTRY_ID)
  lateinit var uuid: UUID

  override fun setLayoutId(): Int {
    return R.layout.activity_entry_detail_new
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
  }

  override fun handleStatusBar() {
    // 这个不使用父类的状态栏
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    ARouter.getInstance().inject(this)
    module = ViewModelProvider(this)[EntryDetailModule::class.java]
    pwEntry = (BaseApp.KDB!!.pm.entries[uuid] as PwEntryV4?)!!
    module.initEntry(pwEntry)
    if (BaseApp.isV4 && pwEntry.parent == BaseApp.KDB!!.pm.recycleBin) {
      isInRecycleBin = true
    }
    handleBg()
    setTopBar()
    listenerSaveFile()
    module.saveRecord()
  }

  private fun handleBg() {
    Glide.with(this).load(IconUtil.getEntryIconDrawable(this, pwEntry))
      .apply(RequestOptions.bitmapTransform(BlurTransformation(10, 3)))
      .into(binding.ivBlur)
  }

  private fun listenerSaveFile() {
    lifecycleScope.launch {
      EntryFileCard.SAVE_FILE_FLOW.collectLatest {
        saveFile.launch(it.first)
        fileSaveCache = it.second
      }
    }
  }

  override fun onStart() {
    super.onStart()
    bindData()
  }

  private fun bindData() {
    setIcon()
    // 处理过期
    KpaUtil.handleExpire(binding.tvTitle, pwEntry)
    binding.tvTitle.text = pwEntry.title
    binding.topAppBar.title = pwEntry.title
    binding.cardBaseInfo.bindData(pwEntry)
    binding.cardNote.bindData(pwEntry)
    binding.cardStr.bindData(pwEntry)
    binding.cardAtta.bindData(pwEntry)
    binding.cardTag.bindData(pwEntry)
  }

  /**
   * 标题栏
   */
  private fun setTopBar() {
    toolbar = binding.topAppBar
    toolbar.setNavigationOnClickListener {
      finishAfterTransition()
    }
    toolbar.inflateMenu(R.menu.menu_entry_detail)
    toolbar.menu.findItem(R.id.collect)
      .setIcon(if (!pwEntry.isCollectioned()) R.drawable.ic_star_outline else R.drawable.ic_star)

    toolbar.setOnMenuItemClickListener { item ->
      if (KeepassAUtil.instance.isFastClick()) {
        return@setOnMenuItemClickListener true
      }
      when (item.itemId) {
        R.id.edit -> {
          Routerfit.create(ActivityRouter::class.java, this).toEditEntryActivity(pwEntry.uuid)
        }

        R.id.collect -> {
          KpaUtil.kdbHandlerService.collection(pwEntry, !pwEntry.isCollectioned())
          item.setIcon(if (!pwEntry.isCollectioned()) R.drawable.ic_star_outline else R.drawable.ic_star)
        }
      }

      true
    }

    binding.appBarLayout.addOnOffsetChangedListener { _, verticalOffset ->
      // Timber.d("offset: $verticalOffset， ${binding.appBarLayout.totalScrollRange}")
      if (verticalOffset == 0) {
        binding.topAppBar.title = ""
        return@addOnOffsetChangedListener
      }
      if (abs(verticalOffset) >= binding.appBarLayout.totalScrollRange) {
        binding.topAppBar.title = pwEntry.title
        binding.clFun.setBackgroundColor(Color.TRANSPARENT)
        binding.clContent.setBackgroundColor(Color.TRANSPARENT)
        binding.appBarLayout.setBackgroundColor(Color.TRANSPARENT)
        return@addOnOffsetChangedListener
      }
    }

    ViewCompat.setOnApplyWindowInsetsListener(binding.topAppBar) { v, windowInsets ->
      val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
      v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        topMargin = insets.top
      }

      WindowInsetsCompat.CONSUMED
    }

    binding.topAppBar.bringToFront()

    handleMenuBar()
  }

  private fun handleMenuBar() {
    binding.btnUserName.doClick {
      pwEntry.copyUserName()
    }
    binding.btnUserPass.doClick {
      pwEntry.copyPassword()
    }
    binding.btnTotp.visibility = if (!pwEntry.hasTOTP()) View.GONE else View.VISIBLE
    binding.btnTotp.doClick {
      pwEntry.copyTotp()
    }
  }

  /**
   * 设置图标
   */
  private fun setIcon() {
    val color = if (pwEntry.getCustomIcon()?.imageData?.isNotEmpty() == true) {
      module.getColor(this, BitmapDrawable(IconUtil.getCustomBitmap(pwEntry)))
    } else {
      val defColor = ResUtil.getColor(R.color.color_444E85DB)
      Pair(defColor, defColor)
    }

    binding.tvChar.visibility = View.VISIBLE
    if (pwEntry.title.isEmpty()) {
      binding.tvChar.text = "#"
    } else {
      binding.tvChar.text = pwEntry.title.substring(0, 1).uppercase(Locale.getDefault())
    }

    val cards = arrayOf(
      binding.cardStr,
      binding.cardBaseInfo,
      binding.cardTag,
      binding.cardAtta,
      binding.cardNote
    )
    binding.ivIcon.setBackgroundColor(color.first)

    cards.forEach {
      it.setBackgroundColor(ColorUtils.setAlphaComponent(color.second, 0.6f))
    }
  }

  private fun setAppIcon() {
    val adapter = AppIconAdapter()

    binding.rvAppIcon.apply {
      this.adapter = adapter
      setChildDrawingOrderCallback { childCount, i ->
        if (childCount <= 1) {
          return@setChildDrawingOrderCallback i
        }
        return@setChildDrawingOrderCallback childCount - i - 1

      }
      layoutManager = AppIconLayoutManager(15.toPx())
    }
    adapter.setData(arrayListOf<String>().apply {
      add("tv.danmaku.bili")
    })
  }
}