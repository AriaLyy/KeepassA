/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.detail

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProvider
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.PwEntryV4
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
import com.lyy.keepassa.widget.toPx
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

  @Autowired(name = KEY_ENTRY_ID)
  lateinit var uuid: UUID

  override fun setLayoutId(): Int {
    return R.layout.activity_entry_detail_new
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
    setTopBar()
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
          Routerfit.create(ActivityRouter::class.java, this).toEditEntryActivity(
            pwEntry.uuid,
            ActivityOptionsCompat.makeSceneTransitionAnimation(this)
          )
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
        return@addOnOffsetChangedListener
      }
    }
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
      ResUtil.getColor(R.color.color_444E85DB)
    }

    binding.tvChar.visibility = View.VISIBLE
    binding.tvChar.text = pwEntry.title.substring(0, 1)

    binding.ivIcon.setBackgroundColor(color)
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