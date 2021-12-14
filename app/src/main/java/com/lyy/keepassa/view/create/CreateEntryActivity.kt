/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.create

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroupId
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
import com.lyy.keepassa.event.EditorEvent
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.EventBusHelper
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.getFileInfo
import com.lyy.keepassa.util.putArgument
import com.lyy.keepassa.util.takePermission
import com.lyy.keepassa.view.MarkDownEditorActivity
import com.lyy.keepassa.view.dialog.AddMoreDialog
import com.lyy.keepassa.view.dialog.CreateTotpDialog
import com.lyy.keepassa.view.dialog.LoadingDialog
import com.lyy.keepassa.view.dialog.OnMsgBtClickListener
import com.lyy.keepassa.view.dir.ChooseGroupActivity
import com.lyy.keepassa.view.icon.IconBottomSheetDialog
import com.lyy.keepassa.view.icon.IconItemCallback
import com.lyy.keepassa.view.launcher.LauncherActivity
import com.lyy.keepassa.view.launcher.LauncherActivity.Companion.KEY_IS_AUTH_FORM_FILL_SAVE
import com.lyy.keepassa.view.menu.EntryCreateFilePopMenu
import com.lyy.keepassa.view.menu.EntryCreateStrPopMenu
import com.lyy.keepassa.widget.expand.AttrFileItemView
import com.lyy.keepassa.widget.expand.AttrStrItemView
import com.lyy.keepassa.widget.expand.ExpandFileAttrView
import com.lyy.keepassa.widget.expand.ExpandStrAttrView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import timber.log.Timber
import java.util.UUID

/**
 * 创建或编辑条目
 */
@Route(path = "/entry/create")
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

    const val IS_SHORTCUTS = "isShortcuts"

    // 新建条目
    const val TYPE_NEW_ENTRY = 1

    // 编辑条目
    const val TYPE_EDIT_ENTRY = 3

  }

  private val passRequestCode = 0xA2
  private val groupDirRequestCode = 0xA3
  private val getFileRequestCode = 0xA4
  private val editorRequestCode = 0xA5

  private var isShowPass = false
  private lateinit var module: CreateEntryModule
  private var addMoreDialog: AddMoreDialog? = null
  private lateinit var addMoreData: ArrayList<SimpleItemEntity>
  private lateinit var pwEntry: PwEntryV4
  private lateinit var loadDialog: LoadingDialog

  @Autowired(name = KEY_IS_AUTH_FORM_FILL_SAVE)
  @JvmField
  var isFromAutoFillSave = false

  @Autowired(name = KEY_ENTRY)
  lateinit var entryId: UUID

  @Autowired(name = KEY_TYPE)
  @JvmField
  var type = 1

  @Autowired(name = PARENT_GROUP_ID)
  @JvmField
  var parentGroupId: PwGroupId? = null

  @Autowired(name = IS_SHORTCUTS)
  @JvmField
  var isShortcuts: Boolean = false

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    ARouter.getInstance().inject(this)
    EventBusHelper.reg(this)
    module = ViewModelProvider(this).get(CreateEntryModule::class.java)
    Timber.i("isShortcuts = $isShortcuts")

    // 处理快捷方式进入的情况
    if (isShortcuts) {
      type = TYPE_NEW_ENTRY
    }

    when (type) {
      TYPE_NEW_ENTRY -> {
        toolbar.title = getString(R.string.create_entry)
      }
      TYPE_EDIT_ENTRY -> {
        toolbar.title = getString(R.string.edit)
      }
    }

    if (type == TYPE_EDIT_ENTRY) {
      val entryTemp = BaseApp.KDB!!.pm.entries[entryId]
      if (entryTemp == null) {
        Timber.e("【${entryId}】对应的条目不存在")
        finish()
        return
      }
      pwEntry = entryTemp as PwEntryV4
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
    handleAddMore()
    if (type == TYPE_EDIT_ENTRY || isFromAutoFillSave) {
      initData(type == TYPE_EDIT_ENTRY)
    } else {
      pwEntry = PwEntryV4(BaseApp.KDB!!.pm.rootGroup as PwGroupV4)
    }
    setWidgetListener()
  }

  /**
   * 设置各种事件
   */
  private fun setWidgetListener() {
    binding.cbLoseTime.setOnCheckedChangeListener { _, isChecked ->
      module.expires = isChecked
      pwEntry.setExpires(isChecked)
    }
    binding.noticeLayout.setOnClickListener {
      MarkDownEditorActivity.turnMarkDownEditor(
        this,
        editorRequestCode,
        module.noteStr
      )
    }
    // the user name field, can show history
    module.getUserNameCache()
      .observe(this, {
        val adapter = ArrayAdapter(this, R.layout.android_simple_dropdown_item_1line, it)
        binding.user.setAdapter(adapter)
        binding.user.threshold = 1 // 设置输入几个字符后开始出现提示 默认是2
        binding.user.setOnFocusChangeListener { _, hasFocus ->
          if (hasFocus) {
            binding.user.showDropDown()
          }
        }
      })

    // lose time modify
    binding.ivLoseTimeClick.setOnClickListener {
      Timber.d("ivLoseTimeClick ")
      showTimeChangeDialog()
    }
  }

  /**
   * time change dialog
   */
  private fun showTimeChangeDialog() {
    val dialog = Routerfit.create(DialogRouter::class.java).toTimeChangeDialog()
    lifecycleScope.launch {
      dialog.timeFlow.collectLatest { event ->
        if (event == null) {
          return@collectLatest
        }
        val time = "${event.year}/${event.month}/${event.dayOfMonth} ${event.hour}:${event.minute}"
        val dateTime = DateTime(
          event.year, event.month, event.dayOfMonth, event.hour, event.minute, DateTimeZone.UTC
        )
        module.loseDate = dateTime.toDate()
        binding.cbLoseTime.text = time
      }
    }
    dialog.show(supportFragmentManager, "timer_dialog")
  }

  /**
   * 初始化数据，根据模版创建或编辑时需要初始化数据
   */
  private fun initData(isEdit: Boolean) {
    if (isEdit) {
      module.icon = pwEntry.icon
      binding.title.setText(pwEntry.title)
      binding.user.setText(pwEntry.username)
    } else {
//      binding.user.setText("newEntry")
      binding.title.setText(getString(R.string.normal_account))
    }
    binding.password.setText(pwEntry.password)
    binding.enterPassword.setText(pwEntry.password)
    binding.url.setText(pwEntry.url)
    binding.titleLayout.endIconDrawable =
      IconUtil.getEntryIconDrawable(this, pwEntry, zoomIcon = true)

    if (pwEntry.notes.isNotEmpty()) {
      module.noteStr = pwEntry.notes.trim()
      binding.noticeLayout.visibility = View.VISIBLE
      binding.notice.text = module.noteStr
    }

    val v4Entry = pwEntry
    module.loseDate = v4Entry.expiryTime
    if (v4Entry.expires()) {
      binding.loseTime.visibility = View.VISIBLE
      binding.cbLoseTime.isChecked = v4Entry.expires()
      binding.cbLoseTime.text = KeepassAUtil.instance.formatTime(v4Entry.expiryTime)
      module.expires = v4Entry.expires()
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
      module.attrFileMap.clear()
      module.attrFileMap.putAll(map)
    }

    val strMap = LinkedHashMap<String, ProtectedString>()
    strMap.putAll(KeepassAUtil.instance.filterCustomStr(v4Entry, false))
    if (strMap.isNotEmpty()) {
      showStrLayout()
      binding.attrStrs.setValue(strMap)
      module.attrStrMap.clear()
      module.attrStrMap.putAll(strMap)
    }
  }

  /**
   * 处理添加更多逻辑
   */
  private fun handleAddMore() {
    binding.addMore.setOnClickListener {
      if (KeepassAUtil.instance.isFastClick()) {
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
                showLoseTimeLayout()
              }
              R.drawable.ic_notice -> {
                MarkDownEditorActivity.turnMarkDownEditor(
                  this@CreateEntryActivity,
                  editorRequestCode,
                  null
                )
              }
              R.drawable.ic_attr_str -> { // 自定义字段
                CreateCustomStrDialog().show()
              }
              R.drawable.ic_attr_file -> { // 附件
                KeepassAUtil.instance.openSysFileManager(
                  this@CreateEntryActivity,
                  "*/*",
                  getFileRequestCode
                )
              }
              R.drawable.ic_totp -> { // totp
                CreateTotpDialog().apply {
                  putArgument("isEdit", false)
                  putArgument("entryTitle", pwEntry.title)
                  putArgument("entryUserName", pwEntry.username)
                }
                  .show()
              }
            }
            addMoreDialog!!.dismiss()
          }
        })
      }

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
      if (module.hasTotp(pwEntry)) {
        addMoreData.remove(addMoreData.find { it.icon == R.drawable.ic_totp })
      }
      addMoreDialog!!.notifyData()
      addMoreDialog!!.show(supportFragmentManager, "add_more_dialog")
    }
  }

  @SuppressLint("SetTextI18n")
  private fun showLoseTimeLayout() {
    binding.loseTime.apply {
      visibility = View.VISIBLE
      requestFocus()
      if (module.loseDate == null) {
        module.loseDate = DateTime(System.currentTimeMillis()).toDate()
      }
      val lt = DateTime(module.loseDate)
      binding.cbLoseTime.text = KeepassAUtil.instance.formatTime(lt.toDate())
    }
    if (binding.otherLine.visibility == View.GONE) {
      binding.otherLine.visibility = View.VISIBLE
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
      val iconDialog = IconBottomSheetDialog()
      iconDialog.setCallback(object : IconItemCallback {
        override fun onDefaultIcon(defIcon: PwIconStandard) {
          module.icon = defIcon
          binding.titleLayout.endIconDrawable =
            resources.getDrawable(IconUtil.getIconById(module.icon.iconId), theme)
          module.customIcon = PwIconCustom.ZERO
        }

        override fun onCustomIcon(customIcon: PwIconCustom) {
          module.customIcon = customIcon
          binding.titleLayout.endIconDrawable =
            IconUtil.convertCustomIcon2Drawable(this@CreateEntryActivity, module.customIcon!!)
        }
      })
      iconDialog.show(supportFragmentManager, IconBottomSheetDialog::class.java.simpleName)
    }
  }

  /**
   * 保存数据库
   */
  private fun save() {
    val pass = binding.password.text.toString()
    val enterPass = binding.enterPassword.text.toString()

    // 显示密码状态，不需要两次确认
    if (pass.isNotEmpty() && pass != enterPass && !isShowPass) {
      HitUtil.toaskShort(getString(R.string.error_pass_unfit))
      return
    }

    if (type == TYPE_NEW_ENTRY) {
      if (parentGroupId == null) {
        ChooseGroupActivity.chooseGroup(this, groupDirRequestCode)
      } else {
        createEntry(parentGroupId!!)
      }
      return
    }
    loadDialog = LoadingDialog(this)
    loadDialog.show()
    module.updateEntry(
      entry = pwEntry,
      title = binding.title.text.toString(),
      userName = binding.user.text.toString(),
      pass = binding.password.text.toString(),
      url = binding.url.text.toString(),
      tags = binding.tag.text.toString()
    )
    module.saveDb()
      .observe(this, { success ->
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
    Routerfit.create(DialogRouter::class.java)
      .toMsgDialog(
        msgTitle = ResUtil.getString(R.string.warning),
        msgContent = ResUtil.getString(R.string.create_entry_no_save),
        btnClickListener = object : OnMsgBtClickListener {
          override fun onCover(v: Button) {
          }

          override fun onEnter(v: Button) {
            finishAfterTransition()
          }

          override fun onCancel(v: Button) {

          }

        }
      )
      .show()
  }

  /**
   * 创建实体
   */
  private fun createEntry(parentId: PwGroupId) {
    pwEntry.parent = BaseApp.KDB!!.pm.groups[parentId] as PwGroupV4?

    module.updateEntry(
      entry = pwEntry,
      title = binding.title.text.toString(),
      userName = binding.user.text.toString(),
      pass = binding.password.text.toString(),
      url = binding.url.text.toString(),
      tags = binding.tag.text.toString()
    )
    loadDialog = LoadingDialog(this)
    loadDialog.show()
    module.addEntry(pwEntry)
      .observe(this, { success ->
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
   * 编辑器返回的文本
   */
  @Subscribe(threadMode = MAIN)
  fun onEditorEvent(event: EditorEvent) {
    if (event.requestCode != editorRequestCode) {
      return
    }
    event.content?.let {
      Timber.d("note = $it")
      module.noteStr = it.trim()
      showOtherItem(binding.noticeLayout, false)
      binding.notice.text = module.noteStr
    }
  }

  /**
   * 创建自定义字段事件
   */
  @Subscribe(threadMode = MAIN)
  fun onCreateAttrStr(event: CreateAttrStrEvent) {
    if (event.isEdit) {
      val oldKey = event.updateView!!.titleStr
      module.attrStrMap.remove(oldKey)
      module.attrStrMap[event.key] = event.str
      binding.attrStrs.updateKeyValue(event.updateView, event.key, event.str)
      return
    }
    showStrLayout()
    binding.attrStrs.addValue(event.key, event.str)
    module.attrStrMap[event.key] = event.str
  }

  /**
   * 删除自定义字段事件
   */
  @Subscribe(threadMode = MAIN)
  fun onDelAttrStr(event: DelAttrStrEvent) {
    binding.attrStrs.removeValue(event.key)
    module.attrStrMap.remove(event.key)
    if (module.attrStrMap.isEmpty()) {
      if (module.attrFileMap.isEmpty()) {
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
    module.attrFileMap.remove(event.key)
    if (module.attrFileMap.isEmpty()) {
      if (module.attrStrMap.isEmpty()) {
        binding.attrLine.visibility = View.GONE
      }
      binding.attrFileLayout.visibility = View.GONE
    }
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
   * 添加附件
   */
  private fun addAttrFile(uri: Uri?) {
    if (uri == null) {
      Timber.e("附件uri为空")
      HitUtil.snackShort(
        rootView,
        "${getString(R.string.add_attr_file)}${getString(R.string.fail)}"
      )
      return
    }
    val fileInfo = uri.getFileInfo(this)
    if (TextUtils.isEmpty(fileInfo.first) || fileInfo.second == null) {
      Timber.e("获取文件名失败")
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
    module.attrFileMap[fileName] = ProtectedBinary(
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
        // 处理获取密码
        passRequestCode -> {
          binding.password.setText(data.getStringExtra(GeneratePassActivity.DATA_PASS_WORD))
          binding.enterPassword.setText(data.getStringExtra(GeneratePassActivity.DATA_PASS_WORD))
        }
        // 处理群组选择
        groupDirRequestCode -> {
          createEntry(data.getSerializableExtra(ChooseGroupActivity.DATA_PARENT) as PwGroupId)
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