/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.launcher

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.text.Html
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.transition.TransitionInflater
import com.arialyy.frame.util.adapter.AbsHolder
import com.arialyy.frame.util.adapter.AbsRVAdapter
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.dropbox.core.android.Auth
import com.keepassdroid.utils.UriUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.R.string
import com.lyy.keepassa.base.BaseFragment
import com.lyy.keepassa.databinding.FragmentChangeDbBinding
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.event.ChangeDbEvent
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KLog
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.KeepassAUtil.takePermission
import com.lyy.keepassa.util.cloud.DropboxUtil
import com.lyy.keepassa.util.putArgument
import com.lyy.keepassa.view.DbPathType
import com.lyy.keepassa.view.DbPathType.AFS
import com.lyy.keepassa.view.DbPathType.DROPBOX
import com.lyy.keepassa.view.DbPathType.WEBDAV
import com.lyy.keepassa.view.create.CreateDbActivity
import com.lyy.keepassa.view.dialog.CloudFileListDialog
import com.lyy.keepassa.view.dialog.MsgDialog
import com.lyy.keepassa.view.dialog.MsgDialog.OnBtClickListener
import com.lyy.keepassa.view.dialog.WebDavLoginDialog
import com.lyy.keepassa.view.launcher.ChangeDbFragment.Adataer.Holder
import org.greenrobot.eventbus.EventBus
import java.util.ArrayList

/**
 * 选择数据库
 */
class ChangeDbFragment : BaseFragment<FragmentChangeDbBinding>() {
  private lateinit var modlue: LauncherModule
  private var dbOpenType: DbPathType = AFS

  companion object {
    val FM_TAG = "ChangeDbFragment"
    const val REQ_CODE_OPEN_DB_BY_AFS = 0xa1
  }

  private var startDropboxAuth = false

  override fun initData() {
    enterTransition = TransitionInflater.from(context)
        .inflateTransition(R.transition.slide_enter)
    exitTransition = TransitionInflater.from(context)
        .inflateTransition(R.transition.slide_exit)
    returnTransition = TransitionInflater.from(context)
        .inflateTransition(R.transition.slide_return)
    initList()
  }

  override fun setLayoutId(): Int {
    return R.layout.fragment_change_db
  }

  private fun initList() {
    modlue = ViewModelProvider(this).get(LauncherModule::class.java)
    val data: ArrayList<SimpleItemEntity> = ArrayList()
    val adapter = Adataer(requireContext(), data)
    binding.list.layoutManager = GridLayoutManager(requireContext(), 4)
    binding.list.adapter = adapter

    modlue.getDbOpenTypeData(requireContext())
        ?.observe(this, Observer { simpleItemDaos ->
          run {
            data.addAll(simpleItemDaos)
            adapter.notifyDataSetChanged()
          }
        })
    RvItemClickSupport.addTo(binding.list)
        .setOnItemClickListener { _, position, _ ->
          if (KeepassAUtil.isFastClick()
              || activity == null
              || position < 0
              || position >= data.size
          ) {
            return@setOnItemClickListener
          }
          when (data[position].id) {
            // 文件系统选择db
            AFS.type -> {
              dbOpenType = AFS
              KeepassAUtil.openSysFileManager(
                  this@ChangeDbFragment, "*/*", REQ_CODE_OPEN_DB_BY_AFS
              )
            }
            DROPBOX.type -> {
              dbOpenType = DROPBOX
              changeDropbox()
            }
            WEBDAV.type -> {
              dbOpenType = WEBDAV
              changeWebDav()
            }
            6 -> { // 历史记录
              startActivity(
                  Intent(context, OpenDbHistoryActivity::class.java),
                  ActivityOptions.makeSceneTransitionAnimation(activity)
                      .toBundle()
              )
            }

          }
        }
    binding.fab.setOnClickListener {
      startActivity(
          Intent(context, CreateDbActivity::class.java),
          ActivityOptions.makeSceneTransitionAnimation(activity)
              .toBundle()
      )
    }
  }

  /**
   * 选择webDav
   */
  private fun changeWebDav() {
    if (!isAdded) {
      KLog.e(TAG, "webDav fragment 还没加载到activity中")
      return
    }
    val dialog = WebDavLoginDialog()
    dialog.show(requireActivity().supportFragmentManager, "web_dav_login")
  }

  /**
   * 选择dropbox数据库
   */
  private fun changeDropbox() {
    if (!isAdded) {
      KLog.e(TAG, "dropbox fragment 还没加载到activity中")
      return
    }
    if (DropboxUtil.isAuth()) {
      showCloudListDialog()
    } else {
      startDropboxAuth = true
      val title = requireActivity().getString(string.hint)
      val msgDialog = MsgDialog.generate {
        msgTitle = title
        msgContent = Html.fromHtml(this@ChangeDbFragment.getString(string.dropbox_msg))
        showCancelBt = true
        interceptBackKey = true
        build()
      }
      msgDialog.setOnBtClickListener(object : OnBtClickListener {
        override fun onBtClick(
          type: Int,
          view: View
        ) {
          if (type == MsgDialog.TYPE_ENTER) {
            Auth.startOAuth2Authentication(context, DropboxUtil.APP_KEY)
          } else {
            startDropboxAuth = false
          }
        }
      })
      msgDialog.show()
    }
  }

  override fun onResume() {
    super.onResume()
    if (startDropboxAuth) {
      val token = Auth.getOAuth2Token()
      if (!TextUtils.isEmpty(token)) {
        DropboxUtil.saveToken(token)
        // 如果授权成功，进入下一步
        showCloudListDialog()
      } else {
        HitUtil.toaskShort("dropbox ${getString(R.string.auth)}${getString(R.string.fail)}")
      }
    }
  }

  /**
   * 显示云端文件列表
   */
  private fun showCloudListDialog() {
    val dialog = CloudFileListDialog().apply {
      putArgument("cloudFileDbPathType", dbOpenType)
    }
    dialog.show(requireActivity().supportFragmentManager, "cloud_file_list_dialog")
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == Activity.RESULT_OK) {
      if (requestCode == REQ_CODE_OPEN_DB_BY_AFS && data != null && data.data != null) {
        // 申请长期的uri权限
        data.data!!.takePermission()
        EventBus.getDefault()
            .post(
                ChangeDbEvent(
                    dbName = UriUtil.getFileNameFromUri(requireContext(), data.data),
                    localFileUri = data.data!!,
                    uriType = AFS
                )
            )
      }
    }
  }

  /**
   * 适配器
   */
  private class Adataer(
    context: Context,
    data: List<SimpleItemEntity>
  ) : AbsRVAdapter<SimpleItemEntity, Holder>(context, data) {

    override fun getViewHolder(
      convertView: View?,
      viewType: Int
    ): Holder {
      return Holder(convertView!!)
    }

    override fun setLayoutId(type: Int): Int {
      return R.layout.item_mian_content
    }

    override fun bindData(
      holder: Holder?,
      position: Int,
      item: SimpleItemEntity?
    ) {
      holder!!.icon.setImageResource(item!!.icon)
      holder.text.text = item.title
    }

    private class Holder(itemView: View) : AbsHolder(itemView) {
      val icon: AppCompatImageView = itemView.findViewById(R.id.img)
      var text: TextView = itemView.findViewById(R.id.text)

    }
  }
}