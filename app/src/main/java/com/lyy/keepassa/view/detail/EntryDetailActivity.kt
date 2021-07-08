/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.detail

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.Html
import android.text.InputType
import android.text.Spanned
import android.util.Pair
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityOptionsCompat
import androidx.core.transition.addListener
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV3
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedBinary
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.databinding.ActivityEntryDetailBinding
import com.lyy.keepassa.event.CreateOrUpdateEntryEvent
import com.lyy.keepassa.event.DelEvent
import com.lyy.keepassa.util.EventBusHelper
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.VibratorUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.takePermission
import com.lyy.keepassa.view.create.CreateEntryActivity
import com.lyy.keepassa.view.dialog.LoadingDialog
import com.lyy.keepassa.view.dialog.MsgDialog
import com.lyy.keepassa.view.menu.EntryDetailStrPopMenu
import com.lyy.keepassa.view.menu.EntryDetailStrPopMenu.OnShowPassCallback
import com.lyy.keepassa.widget.expand.ExpandAttrStrLayout
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import java.util.ArrayList
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

    /**
     * 跳转群组详情或项目详情
     */
    fun toEntryDetail(
      activity: FragmentActivity,
      entry: PwEntry,
      showElement: View? = null
    ) {
      ARouter.getInstance()
        .build("/entry/detail")
        .withSerializable(KEY_ENTRY_ID, entry.uuid)
        .withString(KEY_GROUP_TITLE, entry.parent.name)
        .withOptionsCompat(
          if (showElement != null) {
            val pair = androidx.core.util.Pair(
              showElement, activity.getString(R.string.transition_entry_icon)
            )
            ActivityOptionsCompat.makeSceneTransitionAnimation(activity, pair)
          } else {
            ActivityOptionsCompat.makeSceneTransitionAnimation(activity)
          }
        )
        .navigation(activity)
    }
  }

  private lateinit var module: EntryDetailModule
  private lateinit var pwEntry: PwEntry
  private lateinit var loadDialog: LoadingDialog
  private var isInRecycleBin = false
  private var curTouchX = 0f
  private var curTouchY = 0f

  @Autowired(name = KEY_ENTRY_ID)
  lateinit var uuid: UUID

  @Autowired(name = KEY_GROUP_TITLE)
  lateinit var groupTitle: String

  override fun setLayoutId(): Int {
    return R.layout.activity_entry_detail
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    ARouter.getInstance().inject(this)
    EventBusHelper.reg(this)
    module = ViewModelProvider(this).get(EntryDetailModule::class.java)
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
        CreateEntryActivity.editEntry(this)
      }

      true
    }

    pwEntry = BaseApp.KDB!!.pm.entries[uuid]!!
    module.initEntry(pwEntry)
    if (BaseApp.isV4 && pwEntry.parent == BaseApp.KDB!!.pm.recycleBin) {
      isInRecycleBin = true
    }

    binding.delBt.setOnClickListener {
      delEntry()
    }

    setData()
  }

  override fun useAnim(): Boolean {
    return false
  }

  override fun finishAfterTransition() {
    showContent(false)
    if (!KeepassAUtil.instance.isDisplayLoadingAnim()) {
      super.finishAfterTransition()
      return
    }
    module.finishAnim(this, binding.rlRoot, binding.icon)
      .observe(this, {
        super.finishAfterTransition()
      })
  }

  private fun showContent(show: Boolean) {
    if (show) {
      binding.title.visibility = View.VISIBLE
      binding.kpaToolbar.visibility = View.VISIBLE
      binding.line.visibility = View.VISIBLE
      binding.scrollView.visibility = View.VISIBLE
      binding.bottomBar.visibility = View.VISIBLE
      binding.bottomLine.visibility = View.VISIBLE
      return
    }
    binding.title.visibility = View.GONE
    binding.kpaToolbar.visibility = View.INVISIBLE
    binding.line.visibility = View.INVISIBLE
    binding.scrollView.visibility = View.INVISIBLE
    binding.bottomBar.visibility = View.INVISIBLE
    binding.bottomLine.visibility = View.INVISIBLE
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
          .observe(this, {
            showContent(true)
          })
      })
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

    val dialog = MsgDialog.generate {
      msgTitle = this@EntryDetailActivity.getString(R.string.del_entry)
      msgContent = msg
      build()
    }
    dialog.setOnBtClickListener(object : MsgDialog.OnBtClickListener {
      override fun onBtClick(
        type: Int,
        view: View
      ) {
        if (type == MsgDialog.TYPE_ENTER) {
          loadDialog = LoadingDialog(this@EntryDetailActivity)
          loadDialog.show()
          module.recycleEntry(pwEntry)
            .observe(this@EntryDetailActivity, Observer { code ->
              if (code == DbSynUtil.STATE_SUCCEED) {
                onComplete(pwEntry)
                return@Observer
              }
              HitUtil.toaskShort(getString(R.string.save_db_fail))
              return@Observer
            })
        }
        dialog.dismiss()
      }
    })
    dialog.show()
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
        EntryDetailStrPopMenu(this, v, ProtectedString(false, (pwEntry as PwEntryV4).tags)).show()
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
    }
  }

  /**
   * 更新条目事件
   */
  @Subscribe(threadMode = MAIN)
  fun onUpdateEntryEvent(event: CreateOrUpdateEntryEvent) {
    if (event.entry.uuid != pwEntry.uuid || !event.isUpdate) {
      return
    }
    this.pwEntry = event.entry
    if (binding.attrFile.visibility == View.VISIBLE) {
      binding.attrFile.removeAllViews()
    }
    if (binding.attrStr.visibility == View.VISIBLE) {
      binding.attrStr.removeAllViews()
    }
    setData()
  }

  private fun onComplete(pwEntry: PwEntry) {
    loadDialog.dismiss()
    EventBus.getDefault()
      .post(DelEvent(pwEntry))
    HitUtil.toaskShort(
      "${getString(R.string.del_entry)}${getString(R.string.success)}"
    )
    VibratorUtil.vibrator(300)
    finishAfterTransition()
  }

  override fun onDestroy() {
    if (!isInRecycleBin) {
      // 保存打开历史
      module.saveRecord()
    }
    EventBusHelper.unReg(this)
    super.onDestroy()
  }

  /**
   * 处理时间
   */
  private fun handleTime() {
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

      binding.time1.text = KeepassAUtil.instance.formatTime(pwEntry.creationTime)
      binding.time1.setLeftIcon(R.drawable.ic_create_time)
      binding.time1.setOnClickListener { HitUtil.toaskShort(getString(R.string.create_time)) }
      binding.time2.text = KeepassAUtil.instance.formatTime(pwEntry.lastModificationTime)
      binding.time2.setLeftIcon(R.drawable.ic_modify_time)
      binding.time2.setOnClickListener { HitUtil.toaskShort(getString(R.string.modify_time)) }
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

    if (pwEntry is PwEntryV4) {
      if ((pwEntry as PwEntryV4).tags.isNotEmpty()) {
        binding.tag.visibility = View.VISIBLE
        binding.tag.text = (pwEntry as PwEntryV4).tags
        binding.tag.setOnClickListener(this)
      } else {
        binding.tag.visibility = View.GONE
      }
    } else {
      binding.tag.visibility = View.GONE
    }
  }

  /**
   * 处理高级属性和附件
   */
  private fun handleAttr() {
    // 处理高级属性，只有v4版本的数据库才有
    if (pwEntry is PwEntryV4) {
      binding.attrStr.entryV4 = pwEntry as PwEntryV4
      val data = module.getV4EntryStr(pwEntry as PwEntryV4)
      if (data.isEmpty()) {
        binding.attrStr.visibility = View.GONE
      } else {
        binding.attrStr.visibility = View.VISIBLE
        binding.attrStr.setAttrValue(data)
        setAttrStrListener()
      }
    } else {
      if ((pwEntry as PwEntryV3).additional.isNotEmpty()) {
        binding.attrStr.visibility = View.VISIBLE
        binding.attrStr.addStrValue(
          getString(R.string.hint_ex_property),
          ProtectedString(false, (pwEntry as PwEntryV3).additional)
        )
      } else {
        binding.attrStr.visibility = View.GONE
      }
    }

    // 处理附件，v3 只支持一个附件，v4才支持多附件
    if (pwEntry is PwEntryV4) {
      if ((pwEntry as PwEntryV4).binaries.isNotEmpty()) {
        binding.attrFile.visibility = View.VISIBLE
        binding.attrFile.setFileValue((pwEntry as PwEntryV4).binaries)
        setAttrFileListener()
      } else {
        binding.attrFile.visibility = View.GONE
      }
    } else if (pwEntry is PwEntryV3) {
      val temp = pwEntry as PwEntryV3
      if (temp.binaryDesc.isNotEmpty()) {
        binding.attrFile.visibility = View.VISIBLE
        binding.attrFile.addFileValue(temp.binaryDesc, ProtectedBinary(false, temp.binaryData))
        setAttrFileListener()
      } else {
        binding.attrFile.visibility = View.GONE
      }
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

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == Activity.RESULT_OK
      && data != null
      && data.data != null
      && requestCode == module.createFileRequestCode
      && module.curDLoadFile != null
    ) {
      data.data?.takePermission()
      val dialog = LoadingDialog(this)
      dialog.show()
      module.saveAttachment(this, data.data!!, module.curDLoadFile!!)
        .observe(this, Observer { fileName ->
          dialog.dismiss()
          HitUtil.toaskShort(getString(R.string.save_file_success, fileName))
        })
    }
  }
}