/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.create

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV3
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroupId
import com.keepassdroid.database.PwGroupV3
import com.keepassdroid.database.PwGroupV4
import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.keepassdroid.database.security.ProtectedBinary
import com.keepassdroid.database.security.ProtectedString
import com.keepassdroid.utils.UriUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.databinding.ActivityEntryEditBinding
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.event.CreateAttrStrEvent
import com.lyy.keepassa.event.CreateOrUpdateEntryEvent
import com.lyy.keepassa.event.DelAttrFileEvent
import com.lyy.keepassa.event.DelAttrStrEvent
import com.lyy.keepassa.event.TimeEvent
import com.lyy.keepassa.util.EventBusHelper
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.KeepassAUtil.getFileInfo
import com.lyy.keepassa.util.KeepassAUtil.takePermission
import com.lyy.keepassa.util.putArgument
import com.lyy.keepassa.view.ChooseIconActivity
import com.lyy.keepassa.view.ChoseDirActivity
import com.lyy.keepassa.view.dialog.AddMoreDialog
import com.lyy.keepassa.view.dialog.CreateTotpDialog
import com.lyy.keepassa.view.dialog.LoadingDialog
import com.lyy.keepassa.view.dialog.MsgDialog
import com.lyy.keepassa.view.dialog.TimerDialog
import com.lyy.keepassa.view.launcher.LauncherActivity
import com.lyy.keepassa.view.menu.EntryCreateFilePopMenu
import com.lyy.keepassa.view.menu.EntryCreateStrPopMenu
import com.lyy.keepassa.widget.expand.AttrFileItemView
import com.lyy.keepassa.widget.expand.AttrStrItemView
import com.lyy.keepassa.widget.expand.ExpandFileAttrView
import com.lyy.keepassa.widget.expand.ExpandStrAttrView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.Date
import java.util.UUID

/**
 * 创建或编辑条目
 */
class CreateEntryActivity : BaseActivity<ActivityEntryEditBinding>() {

  companion object {
    const val KEY_ENTRY = "KEY_ENTRY"

    /**
     * 类型，1：新建条目，2：利用模版新建条目，3：编辑条目
     */
    const val KEY_TYPE = "KEY_IS_TYPE"

    /**
     * Entry保存路径
     */
    const val PARENT_GROUP_ID = "PARENT_GROUP_ID"

    // 新建条目
    const val TYPE_NEW_ENTRY = 1

    // 通过模版创建条目
    const val TYPE_NEW_TYPE_ENTRY = 2

    // 编辑条目
    const val TYPE_EDIT_ENTRY = 3
  }

  private val iconRequestCode = 0xA1
  private val passRequestCode = 0xA2
  private val groupDirRequestCode = 0xA3
  private val getFileRequestCode = 0xA4

  private lateinit var entryId: UUID
  private var icon = PwIconStandard(0)
  private var customIcon: PwIconCustom? = null
  private var isShowPass = false
  private var loseDate: Date? = null // 失效时间
  private lateinit var module: CreateEntryModule
  private var addMoreDialog: AddMoreDialog? = null
  private lateinit var addMoreData: ArrayList<SimpleItemEntity>
  private val attrStrMap = LinkedHashMap<String, ProtectedString>()
  private val attrFileMap = LinkedHashMap<String, ProtectedBinary>()
  private var type = 1
  private lateinit var pwEntry: PwEntry
  private var isInitData = false
  private var parentGroupId: PwGroupId? = null
  private lateinit var loadDialog: LoadingDialog
  private var saveDbReqCode = 0xA1
  private var isFromAutoFillSave = false

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    EventBusHelper.reg(this)
    module = ViewModelProvider(this).get(CreateEntryModule::class.java)
    type = intent.getIntExtra(KEY_TYPE, TYPE_NEW_ENTRY)
    isFromAutoFillSave = intent.getBooleanExtra(LauncherActivity.KEY_IS_AUTH_FORM_FILL_SAVE, false)
    val isShortcuts = intent.getBooleanExtra("isShortcuts", false)
    Log.i(TAG, "isShortcuts = $isShortcuts")

    // 处理快捷方式进入的情况
    if (isShortcuts) {
      if (BaseApp.isLocked) {
        Log.w(TAG, "数据库已锁定，进入解锁界面")
        KeepassAUtil.reOpenDb(this)
        finish()
        return
      }
      type = TYPE_NEW_ENTRY
    }

    if (BaseApp.KDB.pm == null){
      HitUtil.toaskShort(getString(R.string.error_entry_id_null))
      finishAfterTransition()
      BaseApp.isLocked = true
      return
    }

    when (type) {
      TYPE_NEW_ENTRY -> {
        toolbar.title = getString(R.string.create_entry)
      }
      TYPE_NEW_TYPE_ENTRY -> {
        toolbar.title = getString(R.string.create_entry)
      }
      TYPE_EDIT_ENTRY -> {
        toolbar.title = getString(R.string.edit)
      }
    }

    if (type == TYPE_NEW_TYPE_ENTRY || type == TYPE_EDIT_ENTRY) {
      val uuidTemp = intent.getSerializableExtra(KEY_ENTRY)
      if (uuidTemp == null) {
        Log.e(TAG, "条目id为-1")
        finish()
        return
      }
      entryId = uuidTemp as UUID
      val entryTemp = BaseApp.KDB.pm.entries[entryId]
      if (entryTemp == null) {
        Log.e(TAG, "【${entryId}】对应的条目不存在")
        finish()
        return
      }
      pwEntry = entryTemp
    } else if (type == TYPE_NEW_ENTRY) {
      val pIdTemp = intent.getSerializableExtra(PARENT_GROUP_ID)
      if (pIdTemp != null) {
        parentGroupId = pIdTemp as PwGroupId
      }
    }

    // 处理从自动填充服务保存的情况
    if (intent.getBooleanExtra(LauncherActivity.KEY_IS_AUTH_FORM_FILL_SAVE, false)) {
      val apkPackageName = intent.getStringExtra(LauncherActivity.KEY_PKG_NAME)
      if (!apkPackageName.isNullOrEmpty()) {
        pwEntry = module.getEntryFromAutoFillSave(
            this,
            apkPackageName,
            intent.getStringExtra(LauncherActivity.KEY_SAVE_USER_NAME),
            intent.getStringExtra(LauncherActivity.KEY_SAVE_PASS)
        )
      }
    }

    handleToolBar()
    handlePassLayout()
    handleOtherWidget()
    handleAddMore()
    if (type == TYPE_NEW_TYPE_ENTRY || type == TYPE_EDIT_ENTRY || isFromAutoFillSave) {
      isInitData = true
      initData()
    } else {
      pwEntry = if (BaseApp.isV4) {
        PwEntryV4(BaseApp.KDB.pm.rootGroup as PwGroupV4)
      } else {
        PwEntryV3(BaseApp.KDB.pm.rootGroup as PwGroupV3)
      }
    }

  }

  /**
   * 初始化数据，根据模版创建或编辑时需要初始化数据
   */
  private fun initData() {
    binding.title.setText(pwEntry.title)
    binding.user.setText(pwEntry.username)
    binding.password.setText(pwEntry.password)
    binding.enterPassword.setText(pwEntry.password)
    binding.url.setText(pwEntry.url)
    binding.titleLayout.endIconDrawable =
      IconUtil.getEntryIconDrawable(this, pwEntry, zoomIcon = true)

    if (pwEntry.notes.isNotEmpty()) {
      binding.notice.visibility = View.VISIBLE
      binding.notice.setText(pwEntry.notes)
    }


    if (BaseApp.isV4) {
      val v4Entry = pwEntry as PwEntryV4
      if (v4Entry.expires()) {
        binding.loseTime.visibility = View.VISIBLE
        binding.loseTime.isChecked = v4Entry.expires()
        binding.loseTime.text = KeepassAUtil.formatTime(v4Entry.expiryTime)
      }
      if (v4Entry.tags.isNotEmpty()) {
        binding.tag.visibility = View.VISIBLE
        binding.tag.setText(v4Entry.tags)
      }
      if (v4Entry.binaries.isNotEmpty()) {
        showFileLayout()
        val map = LinkedHashMap<String, ProtectedBinary>()
        map.putAll(v4Entry.binaries)
        binding.attrFiles.setValue(map)
        attrFileMap.clear()
        attrFileMap.putAll(map)
      }

      val strMap = LinkedHashMap<String, ProtectedString>()
      strMap.putAll(KeepassAUtil.filterCustomStr(v4Entry, false))
      if (strMap.isNotEmpty()) {
        showStrLayout()
        binding.attrStrs.setValue(strMap)
        attrStrMap.clear()
        attrStrMap.putAll(strMap)
      }

    } else {
      val v3Entry = pwEntry as PwEntryV3
      if (v3Entry.expiryTime != PwEntryV3.DEFAULT_DATE) {
        binding.loseTime.visibility = View.VISIBLE
        binding.loseTime.isChecked = v3Entry.expires()
        binding.loseTime.text = KeepassAUtil.formatTime(v3Entry.expiryTime)
      }
      if (v3Entry.binaryData.isNotEmpty()) {
        showFileLayout()
        binding.attrFiles.addValue(v3Entry.binaryDesc, ProtectedBinary(false, v3Entry.binaryData))
      }
    }
  }

  /**
   * 处理其它控件
   */
  private fun handleOtherWidget() {
    binding.loseTime.setOnCheckedChangeListener { buttonView, isChecked ->
      if (isChecked && !isInitData) {
        val dialog = TimerDialog()
        dialog.setOnDismissListener {
          if (loseDate == null) {
            binding.loseTime.isChecked = false
          }
        }
        dialog.show(supportFragmentManager, "timer_dialog")
      }
    }
    binding.user.setText("newEntry")
    binding.title.setText(getString(R.string.normal_account))
    if (!BaseApp.isV4) {
      binding.tagLayout.visibility = View.GONE
      binding.coverUrlLayout.visibility = View.GONE
    }

  }

  /**
   * 处理添加更多逻辑
   */
  private fun handleAddMore() {
    binding.addMore.setOnClickListener {
      if (KeepassAUtil.isFastClick()) {
        return@setOnClickListener
      }
      if (addMoreDialog == null) {
        addMoreData = module.getMoreItem(this)
        addMoreDialog = AddMoreDialog(addMoreData)
        addMoreDialog!!.setOnItemClickListener(object : AddMoreDialog.OnItemClickListener {
          override fun onItemClick(
            position: Int,
            item: SimpleItemEntity,
            view: View
          ) {
            when (item.icon) {
              R.drawable.ic_tag -> {
                showOtherItem(binding.tagLayout)
              }
              R.drawable.ic_net -> {
                showOtherItem(binding.coverUrlLayout)
              }
              R.drawable.ic_lose_time -> {
                showOtherItem(binding.loseTime)
                binding.loseTime.isChecked = true
              }
              R.drawable.ic_notice -> {
                showOtherItem(binding.noticeLayout, false)
              }
              R.drawable.ic_attr_str -> { // 自定义字段
                CreateCustomStrDialog().show()
              }
              R.drawable.ic_attr_file -> { // 附件
                KeepassAUtil.openSysFileManager(this@CreateEntryActivity, "*/*", getFileRequestCode)
              }
              R.drawable.ic_totp -> { // totp
                CreateTotpDialog().apply {
                  putArgument("isEdit", false)
                  putArgument("entryTitle", pwEntry.title)
                  putArgument("entryUserName", pwEntry.username)
                }.show()
              }
            }
            addMoreDialog!!.dismiss()
          }
        })
      }

      if (BaseApp.isV4) {
        if (binding.tagLayout.isVisible) {
          addMoreData.remove(addMoreData.find { it.icon == R.drawable.ic_tag })
        }
        if (binding.coverUrlLayout.isVisible) {
          addMoreData.remove(addMoreData.find { it.icon == R.drawable.ic_net })
        }
        if (binding.loseTime.isVisible) {
          addMoreData.remove(addMoreData.find { it.icon == R.drawable.ic_lose_time })
        }
        if (binding.noticeLayout.isVisible) {
          addMoreData.remove(addMoreData.find { it.icon == R.drawable.ic_notice })
        }
        if (module.hasTotp(pwEntry as PwEntryV4)){
          addMoreData.remove(addMoreData.find { it.icon == R.drawable.ic_totp })
        }
        addMoreDialog!!.notifyData()
      }
      addMoreDialog!!.show(supportFragmentManager, "add_more_dialog")
    }
  }

  /**
   * 显示其它属性的item
   */
  private fun showOtherItem(
    view: View,
    showLine: Boolean = true
  ) {
    view.visibility = View.VISIBLE
    if (showLine && binding.otherLine.visibility == View.GONE) {
      binding.otherLine.visibility = View.VISIBLE
    }
    view.requestFocus()
  }

  /**
   * 处理toolbar
   */
  private fun handleToolBar() {

    toolbar.inflateMenu(R.menu.menu_entry_edit)

    toolbar.setOnMenuItemClickListener { meunItem ->
      when (meunItem.itemId) {
        R.id.cancel -> finishAfterTransition()
        R.id.save -> {
          save()
        }
      }
      true
    }

    binding.titleLayout.setEndIconOnClickListener {
      startActivityForResult(
          Intent(this, ChooseIconActivity::class.java), iconRequestCode,
          ActivityOptions.makeSceneTransitionAnimation(this)
              .toBundle()
      )
    }
  }

  /**
   * 保存数据库
   */
  private fun save() {
    val pass = binding.password.text.toString()
    val enterPass = binding.enterPassword.text.toString()

    if (pass.isEmpty()) {
      HitUtil.toaskShort(getString(R.string.error_pass_null))
      return
    }

    // 显示密码状态，不需要两次确认
    if (pass.isNotEmpty() && pass != enterPass && !isShowPass) {
      HitUtil.toaskShort(getString(R.string.error_pass_unfit))
      return
    }

    if (type == TYPE_NEW_ENTRY || type == TYPE_NEW_TYPE_ENTRY) {
      if (parentGroupId == null) {
        val intent = Intent(this, ChoseDirActivity::class.java)
        intent.putExtra(ChoseDirActivity.KEY_TYPE, 3)
        startActivityForResult(
            intent, groupDirRequestCode,
            ActivityOptions.makeSceneTransitionAnimation(this)
                .toBundle()
        )
      } else {
        createEntry(parentGroupId!!)
      }
      return
    }
    loadDialog = LoadingDialog(this)
    loadDialog.show()
    updateEntry(pwEntry)
    module.saveDb()
        .observe(this, Observer { success ->
          EventBus.getDefault()
              .post(CreateOrUpdateEntryEvent(pwEntry, true))
          loadDialog.dismiss()
          if (!success) {
            HitUtil.toaskLong(getString(R.string.save_db_fail))
          } else {
            finishAfterTransition()
          }
        })
  }

  override fun onBackPressed() {
    val msgDialog = MsgDialog.generate {
      msgTitle = this@CreateEntryActivity.getString(R.string.warning)
      msgContent = this@CreateEntryActivity.getString(R.string.create_entry_no_save)
      build()
    }
    msgDialog.setOnBtClickListener(object : MsgDialog.OnBtClickListener {
      override fun onBtClick(
        type: Int,
        view: View
      ) {
        if (type == MsgDialog.TYPE_ENTER) {
          finishAfterTransition()
        }
      }
    })
    msgDialog.show()
  }

  /**
   * 更新实体
   */
  private fun updateEntry(entry: PwEntry) {
    val title = binding.title.text.toString()
    val userName = binding.user.text.toString()
    val pass = binding.password.text.toString()
    val notes = binding.notice.text.toString()
    val url = binding.url.text.toString()

    if (BaseApp.isV4) {
      if (customIcon != null) {
        (entry as PwEntryV4).customIcon = customIcon
      }
      (entry as PwEntryV4).tags = binding.tag.text.toString()
      if (attrStrMap.isNotEmpty()) {
        entry.strings.clear()
        entry.strings.putAll(attrStrMap)
      } else {
        entry.strings.clear()
      }
      if (attrFileMap.isNotEmpty()) {
        val binPool = (BaseApp.KDB.pm as PwDatabaseV4).binPool
        entry.binaries.clear()
        for (d in attrFileMap) {
          entry.binaries[d.key] = d.value
          if (binPool.poolFind(d.value) == -1) {
            binPool.poolAdd(d.value)
          }
        }
      } else {
        entry.binaries.clear()
      }
    } else {
      if (attrFileMap.isNotEmpty()) {
        for (d in attrFileMap) {
          (entry as PwEntryV3).binaryDesc = d.key
          val byte = d.value.data.readBytes()
          entry.setBinaryData(byte, 0, byte.size)
          break
        }
      }
    }
    entry.setTitle(title, BaseApp.KDB.pm)
    entry.setUsername(userName, BaseApp.KDB.pm)
    entry.setPassword(pass, BaseApp.KDB.pm)
    entry.setUrl(url, BaseApp.KDB.pm)
    entry.setNotes(notes, BaseApp.KDB.pm)
    entry.setExpires(binding.loseTime.isChecked)
    if (binding.loseTime.isChecked) {
      entry.expiryTime = loseDate
    }
    entry.icon = icon
  }

  /**
   * 创建实体
   */
  private fun createEntry(parentId: PwGroupId) {
    pwEntry.parent = BaseApp.KDB.pm.groups[parentId]

    updateEntry(pwEntry)
    loadDialog = LoadingDialog(this)
    loadDialog.show()
    module.addEntry(pwEntry)
        .observe(this, Observer { success ->
          EventBus.getDefault()
              .post(CreateOrUpdateEntryEvent(pwEntry, false))
          loadDialog.dismiss()
          if (!success) {
            HitUtil.toaskLong(getString(R.string.save_db_fail))
          } else {
            finishAfterTransition()
          }
        })
  }

  /**
   * 处理密码
   */
  private fun handlePassLayout() {
    binding.passwordLayout.endIconDrawable = resources.getDrawable(R.drawable.ic_view_off)

    binding.passwordLayout.setEndIconOnClickListener {
      isShowPass = !isShowPass
      if (isShowPass) {
        binding.passwordLayout.endIconDrawable = resources.getDrawable(R.drawable.ic_view)
        binding.enterPasswordLayout.visibility = View.GONE
        binding.password.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
      } else {
        binding.passwordLayout.endIconDrawable =
          resources.getDrawable(R.drawable.ic_view_off)
        binding.enterPasswordLayout.visibility = View.VISIBLE
        binding.password.inputType =
          InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
      }
      // 将光标移动到最后
      binding.password.setSelection(binding.password.text!!.length)
      binding.password.requestFocus()
    }
    binding.passGenerate.setOnClickListener {
      startActivityForResult(
          Intent(this, GeneratePassActivity::class.java),
          passRequestCode,
          ActivityOptions.makeSceneTransitionAnimation(this)
              .toBundle()
      )
    }
  }

  /**
   * 获取时间事件
   */
  @Subscribe(threadMode = MAIN)
  fun onTimeEvent(event: TimeEvent) {
    val time = "${event.year}/${event.month}/${event.dayOfMonth} ${event.hour}:${event.minute}"
    val dateTime = DateTime(
        event.year, event.month, event.dayOfMonth, event.hour, event.minute, DateTimeZone.UTC
    )
    loseDate = dateTime.toDate()
    binding.loseTime.text = time
  }

  /**
   * 创建自定义字段事件
   */
  @Subscribe(threadMode = MAIN)
  fun onCreateAttrStr(event: CreateAttrStrEvent) {
    if (event.isEdit) {
      val oldKey = event.updateView!!.titleStr
      attrStrMap.remove(oldKey)
      attrStrMap[event.key] = event.str
      binding.attrStrs.updateKeyValue(event.updateView, event.key, event.str)
      return
    }
    showStrLayout()
    binding.attrStrs.addValue(event.key, event.str)
    attrStrMap[event.key] = event.str
  }

  /**
   * 显示自定义字段布局
   */
  private fun showStrLayout() {
    if (binding.attrLine.visibility == View.GONE) {
      binding.attrLine.visibility = View.VISIBLE
    }
    if (binding.attrStrLayout.visibility == View.GONE) {
      binding.attrStrLayout.visibility = View.VISIBLE
      binding.attrStrs.setOnStrViewClickListener(object :
          ExpandStrAttrView.OnAttrStrViewClickListener {
        override fun onClickListener(
          v: AttrStrItemView,
          key: String,
          str: ProtectedString,
          position: Int
        ) {
          val menu = EntryCreateStrPopMenu(this@CreateEntryActivity, v, key, str)
          menu.show()
        }
      })
    }
  }

  /**
   * 删除自定义字段事件
   */
  @Subscribe(threadMode = MAIN)
  fun onDelAttrStr(event: DelAttrStrEvent) {
    binding.attrStrs.removeValue(event.key)
    attrStrMap.remove(event.key)
    if (attrStrMap.isEmpty()) {
      if (attrFileMap.isEmpty()) {
        binding.attrLine.visibility = View.GONE
      }
      binding.attrStrLayout.visibility = View.GONE
    }
  }

  /**
   * 删除附件事件
   */
  @Subscribe(threadMode = MAIN)
  fun onDelAttrFile(event: DelAttrFileEvent) {
    binding.attrFiles.removeValue(event.key)
    attrFileMap.remove(event.key)
    if (attrFileMap.isEmpty()) {
      if (attrStrMap.isEmpty()) {
        binding.attrLine.visibility = View.GONE
      }
      binding.attrFileLayout.visibility = View.GONE
    }
  }

  /**
   * 添加附件
   */
  private fun addAttrFile(uri: Uri?) {
    if (uri == null) {
      Log.e(TAG, "附件uri为空")
      HitUtil.snackShort(
          rootView,
          "${getString(R.string.add_attr_file)}${getString(R.string.fail)}"
      )
      return
    }
    val fileInfo = uri.getFileInfo(this)
    if (TextUtils.isEmpty(fileInfo.first) || fileInfo.second == null) {
      Log.e(TAG, "获取文件名失败")
      HitUtil.snackShort(
          rootView,
          "${getString(R.string.add_attr_file)}${getString(R.string.fail)}"
      )
      return
    }
    val fileName = fileInfo.first!!
    val fileSize = fileInfo.second!!
    showFileLayout()
    if (fileSize >= 1024 * 1024 * 10) {
      HitUtil.snackShort(rootView, getString(R.string.error_attr_file_too_large))
      return
    }
    binding.attrFiles.addValue(fileName, fileUri = uri)
    attrFileMap[fileName] = ProtectedBinary(
        false, UriUtil.getUriInputStream(this, uri)
        .readBytes()
    )
  }

  /**
   * 显示附件布局
   */
  private fun showFileLayout() {
    if (binding.attrLine.visibility == View.GONE) {
      binding.attrLine.visibility = View.VISIBLE
    }
    if (binding.attrFileLayout.visibility == View.GONE) {
      binding.attrFileLayout.visibility = View.VISIBLE
      binding.attrFiles.setOnAttrFileViewClickListener(object :
          ExpandFileAttrView.OnAttrFileViewClickListener {
        override fun onClickListener(
          v: AttrFileItemView,
          key: String,
          position: Int
        ) {
          val menu = EntryCreateFilePopMenu(this@CreateEntryActivity, v, key)
          menu.show()
        }
      })
    }
  }

  override fun setLayoutId(): Int {
    return R.layout.activity_entry_edit
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == Activity.RESULT_OK && data != null) {
      when (requestCode) {
        // 处理图标
        iconRequestCode -> {
          val type =
            data.getIntExtra(
                ChooseIconActivity.KEY_ICON_TYPE,
                ChooseIconActivity.ICON_TYPE_STANDARD
            )
          if (type == ChooseIconActivity.ICON_TYPE_STANDARD) {
            icon = data.getSerializableExtra(ChooseIconActivity.KEY_DATA) as PwIconStandard
            binding.titleLayout.endIconDrawable =
              resources.getDrawable(IconUtil.getIconById(icon.iconId), theme)
            customIcon = PwIconCustom.ZERO
            return
          }
          if (type == ChooseIconActivity.ICON_TYPE_CUSTOM) {
            customIcon =
              data.getSerializableExtra(ChooseIconActivity.KEY_DATA) as PwIconCustom
            binding.titleLayout.endIconDrawable =
              IconUtil.convertCustomIcon2Drawable(this, customIcon!!)
          }
        }
        // 处理获取密码
        passRequestCode -> {
          binding.password.setText(data.getStringExtra(GeneratePassActivity.DATA_PASS_WORD))
          binding.enterPassword.setText(data.getStringExtra(GeneratePassActivity.DATA_PASS_WORD))
        }
        // 处理群组选择
        groupDirRequestCode -> {
          createEntry(data.getSerializableExtra(ChoseDirActivity.DATA_PARENT) as PwGroupId)
        }
        // 处理附件
        getFileRequestCode -> {
          data.data?.takePermission()
          addAttrFile(data.data)
        }
      }
    }

  }

  override fun onDestroy() {
    super.onDestroy()
    EventBusHelper.unReg(this)
  }
}