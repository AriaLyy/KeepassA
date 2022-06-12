/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.detail

import android.annotation.SuppressLint
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.text.Html
import android.text.InputType
import android.text.Spanned
import android.util.Pair
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityOptionsCompat
import androidx.core.transition.addListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.databinding.ActivityEntryDetailBinding
import com.lyy.keepassa.event.EntryState.MODIFY
import com.lyy.keepassa.router.ActivityRouter
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.isCollection
import com.lyy.keepassa.util.takePermission
import com.lyy.keepassa.view.dialog.OnMsgBtClickListener
import com.lyy.keepassa.view.menu.EntryDetailStrPopMenu
import com.lyy.keepassa.view.menu.EntryDetailStrPopMenu.OnShowPassCallback
import com.lyy.keepassa.widget.expand.ExpandAttrStrLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import java.util.UUID

/**
 * 项目详情
 */
@Route(path = "/entry/detail")
class EntryDetailActivity : BaseActivity<ActivityEntryDetailBinding>(), View.OnClickListener {
  companion object {
    const val KEY_GROUP_TITLE = "KEY_GROUP_TITLE"
    const val KEY_ENTRY_ID = "KEY_ENTRY_ID"
  }

  private lateinit var module: EntryDetailModule
  private lateinit var pwEntry: PwEntryV4
  private var isInRecycleBin = false
  private var curTouchX = 0f
  private var curTouchY = 0f

  @Autowired(name = KEY_ENTRY_ID)
  lateinit var uuid: UUID

  @Autowired(name = KEY_GROUP_TITLE)
  lateinit var groupTitle: String

  val saveAttachmentResult =
    registerForActivityResult(ActivityResultContracts.CreateDocument()) { resultUri ->
      resultUri.takePermission()
      module.saveAttachment(this, resultUri, module.curDLoadFile!!)
    }

  override fun setLayoutId(): Int {
    return R.layout.activity_entry_detail
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    ARouter.getInstance().inject(this)
    module = ViewModelProvider(this)[EntryDetailModule::class.java]
    val toolbar = findViewById<Toolbar>(R.id.kpa_toolbar)
    toolbar.title = groupTitle
    toolbar.setNavigationOnClickListener {
      finishAfterTransition()
    }
    toolbar.inflateMenu(R.menu.menu_entry_detail)
    toolbar.setOnMenuItemClickListener { item ->
      if (KeepassAUtil.instance.isFastClick()) {
        return@setOnMenuItemClickListener true
      }
      if (item.itemId == R.id.history) {
        // todo 查看历史
      } else if (item.itemId == R.id.edit) {
        Routerfit.create(ActivityRouter::class.java, this).toEditEntryActivity(
          pwEntry.uuid,
          ActivityOptionsCompat.makeSceneTransitionAnimation(this)
        )
      }

      true
    }

    pwEntry = (BaseApp.KDB!!.pm.entries[uuid] as PwEntryV4?)!!
    module.initEntry(pwEntry)
    if (BaseApp.isV4 && pwEntry.parent == BaseApp.KDB!!.pm.recycleBin) {
      isInRecycleBin = true
    }

    binding.delBt.setOnClickListener {
      delEntry()
    }

    setData()
    listenerEntryStateChange()
  }

  /**
   * listener the entry status change, there are three states: create, delete, and modify.
   */
  private fun listenerEntryStateChange() {
    lifecycleScope.launch {
      KpaUtil.kdbHandlerService.entryStateChangeFlow.collectLatest {
        it.pwEntryV4?.let { entry ->
          if (entry.uuid == pwEntry.uuid && it.state == MODIFY) {
            if (binding.attrFile.visibility == View.VISIBLE) {
              binding.attrFile.removeAllViews()
            }
            if (binding.attrStr.visibility == View.VISIBLE) {
              binding.attrStr.removeAllViews()
            }
            setData()
          }
        }
      }
    }
  }

  override fun useAnim(): Boolean {
    return false
  }

  override fun finishAfterTransition() {
    showContent(false)
    if (module.lastCollection != pwEntry.isCollection()) {
      KpaUtil.kdbHandlerService.saveDbByBackground()
    }
    if (!KeepassAUtil.instance.isDisplayLoadingAnim()) {
      super.finishAfterTransition()
      return
    }
    module.finishAnim(this, binding.rlRoot, binding.icon)
      .observe(this) {
        super.finishAfterTransition()
      }
  }

  private fun showContent(show: Boolean) {
    binding.groupContent.visibility = if (show) View.VISIBLE else View.INVISIBLE
  }

  override fun buildSharedElements(vararg sharedElements: Pair<View, String>): ArrayList<String> {
    return super.buildSharedElements(
      Pair<View, String>(
        binding.icon,
        getString(R.string.transition_entry_icon)
      )
    )
  }

  private fun setData() {
    IconUtil.setEntryIcon(this, pwEntry, binding.icon)
    handleBaseAttr()
    handleAttr()
    handleTime()

    if (window.sharedElementEnterTransition == null || !KeepassAUtil.instance.isDisplayLoadingAnim()) {
      showContent(true)
      return
    }

    window.sharedElementEnterTransition?.addListener(
      onStart = {
        showContent(false)
      },
      onEnd = {
        module.startAnim(this, binding.rlRoot, binding.icon)
          .observe(this) {
            showContent(true)
          }
      })
    pwEntry.isCollection().let {
      module.lastCollection = it
      binding.ivCollection.isSelected = it
    }
  }

  /**
   * 删除项目
   */
  @SuppressLint("StringFormatMatches")
  private fun delEntry() {
    val msg: Spanned = if (BaseApp.isV4) {
      if (isInRecycleBin) {
        Html.fromHtml(getString(R.string.hint_del_entry_no_recycle, pwEntry.title))
      } else {
        Html.fromHtml(getString(R.string.hint_del_entry, pwEntry.title, pwEntry.title))
      }
    } else {
      Html.fromHtml(getString(R.string.hint_del_entry_no_recycle, pwEntry.title))
    }

    Routerfit.create(DialogRouter::class.java)
      .showMsgDialog(
        msgTitle = ResUtil.getString(R.string.del_entry),
        msgContent = msg,
        btnClickListener = object : OnMsgBtClickListener {
          override fun onCover(v: Button) {
          }

          override fun onEnter(v: Button) {
            module.recycleEntry(this@EntryDetailActivity, pwEntry)
          }

          override fun onCancel(v: Button) {
          }
        }
      )
  }

  override fun onClick(v: View?) {
    when (v!!.id) {
      R.id.user_name -> {
        EntryDetailStrPopMenu(this, v, ProtectedString(false, KdbUtil.getUserName(pwEntry))).show()
      }
      R.id.url -> {
        EntryDetailStrPopMenu(this, v, ProtectedString(false, pwEntry.url)).show()
      }
      R.id.tag -> {
        EntryDetailStrPopMenu(this, v, ProtectedString(false, pwEntry.tags)).show()
      }
      R.id.notice, R.id.notice_layout -> {
        val pop = EntryDetailStrPopMenu(this, v, ProtectedString(false, pwEntry.notes))
        val rect = Rect()
        v.getLocalVisibleRect(rect)
        if (curTouchX != 0f && curTouchY != 0f && rect.top != 0) {
          pop.show(curTouchX.toInt(), curTouchY.toInt())
          return
        }
        pop.show()
      }
      R.id.pass -> {
        val pop =
          EntryDetailStrPopMenu(this, v, ProtectedString(true, KdbUtil.getPassword(pwEntry)))
        pop.setOnShowPassCallback(object : OnShowPassCallback {
          override fun showPass(showPass: Boolean) {
            if (showPass) {
              binding.pass.inputType =
                (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
            } else {
              binding.pass.inputType =
                (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
            }
          }
        })
        if (binding.pass.inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
          pop.setHidePass()
        }
        pop.show()
      }
      R.id.ivCollection -> {
        if (pwEntry.isCollection()) {
          Timber.d("取消收藏")
          KpaUtil.kdbHandlerService.collection(pwEntry, false)
          binding.ivCollection.isSelected = false
          return
        }

        Timber.d("收藏")
        KpaUtil.kdbHandlerService.collection(pwEntry, true)
        binding.ivCollection.isSelected = true
      }
    }
  }

  override fun onDestroy() {
    if (!isInRecycleBin) {
      // 保存打开历史
      if (this::module.isInitialized) {
        module.saveRecord()
      }
    }
    super.onDestroy()
  }

  /**
   * 处理时间
   */
  private fun handleTime() {
    // 处理已过期
    if (pwEntry.expires() && pwEntry.expiryTime != null) {
      if (pwEntry.expiryTime.after(Date())) {
        binding.time.text =
          getString(R.string.expire_time, KeepassAUtil.instance.formatTime(pwEntry.expiryTime))
      } else {
        binding.time.text = Html.fromHtml(
          getString(
            R.string.expire,
            KeepassAUtil.instance.formatTime(pwEntry.expiryTime, "yyyy/MM/dd")
          )
        )
      }

      val createTime = KeepassAUtil.instance.formatTime(pwEntry.creationTime)
      binding.time1.text = createTime
      binding.time1.setLeftIcon(R.drawable.ic_create_time)
      binding.time1.setOnClickListener {
        HitUtil.toaskShort(
          getString(
            R.string.create_time,
            createTime
          )
        )
      }
      binding.time2.text = KeepassAUtil.instance.formatTime(pwEntry.lastModificationTime)
      binding.time2.setLeftIcon(R.drawable.ic_modify_time)
      binding.time2.setOnClickListener { HitUtil.toaskShort(getString(R.string.modify_time)) }

      // 标题横线
      if (pwEntry.expiryTime.before(Date(System.currentTimeMillis()))) {
        val paint = binding.title.paint
        paint.flags = Paint.STRIKE_THRU_TEXT_FLAG
        paint.isAntiAlias = true
      }
    } else {
      binding.time.text =
        getString(R.string.create_time, KeepassAUtil.instance.formatTime(pwEntry.creationTime))
      binding.time1.text = KeepassAUtil.instance.formatTime(pwEntry.expiryTime)
      binding.time1.setLeftIcon(R.drawable.ic_lose_time)
      binding.time1.setOnClickListener { HitUtil.toaskShort(getString(R.string.lose_time)) }
      binding.time2.text = KeepassAUtil.instance.formatTime(pwEntry.lastModificationTime)
      binding.time2.setLeftIcon(R.drawable.ic_modify_time)
      binding.time2.setOnClickListener { HitUtil.toaskShort(getString(R.string.modify_time)) }
    }

    if (binding.attrFile.visibility == View.GONE && binding.attrStr.visibility == View.GONE) {
      binding.timeLine.visibility = View.GONE
    }
  }

  /**
   * 设置基础属性
   */
  @SuppressLint("SetTextI18n")
  private fun handleBaseAttr() {
    binding.title.text = pwEntry.title
    binding.userName.text = pwEntry.username

    binding.userName.setOnClickListener(this)
    if (pwEntry.url.isNotEmpty()) {
      binding.url.text = pwEntry.url
      binding.url.setOnClickListener(this)
    } else {
      binding.url.visibility = View.GONE
    }

    if (pwEntry.password.isNotEmpty()) {
      binding.pass.text = pwEntry.password
      binding.pass.inputType = (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
      binding.pass.setOnClickListener(this)
    } else {
      binding.pass.visibility = View.GONE
    }

    if (pwEntry.notes.isEmpty()) {
      binding.noticeLayout.visibility = View.GONE
    } else {
      binding.notice.text = pwEntry.notes.trim()
//      binding.notice.setOnClickListener(this)
      binding.noticeLayout.setOnClickListener(this)
      binding.noticeLayout.setOnTouchListener { _, event ->
        curTouchX = event.x
        curTouchY = event.y
        false
      }
    }

    if (pwEntry.tags.isNotEmpty()) {
      binding.tag.visibility = View.VISIBLE
      binding.tag.text = pwEntry.tags
      binding.tag.setOnClickListener(this)
    } else {
      binding.tag.visibility = View.GONE
    }
    binding.ivCollection.setOnClickListener(this)
  }

  /**
   * 处理高级属性和附件
   */
  private fun handleAttr() {
    // 处理高级属性，只有v4版本的数据库才有
    binding.attrStr.entryV4 = pwEntry
    val data = module.getV4EntryStr(pwEntry)
    if (data.isEmpty()) {
      binding.attrStr.visibility = View.GONE
    } else {
      binding.attrStr.visibility = View.VISIBLE
      binding.attrStr.setAttrValue(data)
      setAttrStrListener()
    }

    // 处理附件，v3 只支持一个附件，v4才支持多附件
    if (pwEntry.binaries.isNotEmpty()) {
      binding.attrFile.visibility = View.VISIBLE
      binding.attrFile.setFileValue(pwEntry.binaries)
      setAttrFileListener()
    } else {
      binding.attrFile.visibility = View.GONE
    }
  }

  private fun setAttrStrListener() {
    binding.attrStr.setOnAttrViewClickListener(object :
      ExpandAttrStrLayout.OnAttrViewClickListener {
      override fun onClickListener(
        v: View,
        position: Int
      ) {
        module.showAttrStrPopMenu(this@EntryDetailActivity, v)
      }
    })
  }

  /**
   * 设置附件点击事件
   */
  private fun setAttrFileListener() {
    binding.attrFile.setOnAttrViewClickListener(object :
      ExpandAttrStrLayout.OnAttrViewClickListener {
      override fun onClickListener(
        v: View,
        position: Int
      ) {
        module.showAttrFilePopMenu(this@EntryDetailActivity, v)
      }
    })
  }
}