/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.dialog

import android.content.Context
import android.content.res.AssetManager
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.RelativeLayout.CENTER_VERTICAL
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.arialyy.frame.util.adapter.AbsHolder
import com.arialyy.frame.util.adapter.AbsRVAdapter
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogCloudFileListBinding
import com.lyy.keepassa.event.ChangeDbEvent
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.cloud.CloudFileInfo
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.getArgument
import com.lyy.keepassa.view.StorageType.DROPBOX
import com.lyy.keepassa.view.dialog.CloudFileListDialog.Adapter.Holder
import org.greenrobot.eventbus.EventBus
import java.io.IOException
import java.util.Stack

/**
 * 云文件列表
 */
class CloudFileListDialog : BaseDialog<DialogCloudFileListBinding>() {

  private val curDirList = ArrayList<CloudFileInfo>()
  private lateinit var adapter: Adapter
  private lateinit var module: CloudFileListModule
  private val pathStack = Stack<String>()
  private var lastPath: String = ""
  private val storageType by lazy {
    getArgument("cloudFileDbPathType") ?: DROPBOX
  }

  override fun setLayoutId(): Int {
    return R.layout.dialog_cloud_file_list
  }

  override fun initData() {
    super.initData()
    module = ViewModelProvider(this).get(CloudFileListModule::class.java)
    adapter = Adapter(requireContext(), curDirList)
    binding.list.adapter = adapter
    binding.list.setHasFixedSize(true)
    binding.list.layoutManager = LinearLayoutManager(context)
    RvItemClickSupport.addTo(binding.list)
        .setOnItemClickListener { _, position, _ ->
          val item = curDirList[position]
          if (item.isDir) {
            pathStack.push(lastPath)
            lastPath = item.fileKey
            getFileList(item.fileKey)
            return@setOnItemClickListener
          }
          // 选择文件
          EventBus.getDefault()
              .post(
                  ChangeDbEvent(
                      dbName = item.fileName,
                      localFileUri = DbSynUtil.getCloudDbTempPath(
                          storageType.name,
                          item.fileName
                      ),
                      cloudPath = item.fileKey,
                      uriType = storageType
                  )
              )
          dismiss()
        }
    dialog!!.setOnKeyListener { _, keyCode, _ ->
      if (keyCode == KeyEvent.KEYCODE_BACK && pathStack.size > 0) {
        lastPath = pathStack.pop()
        getFileList(lastPath)
        return@setOnKeyListener true
      }
      return@setOnKeyListener false
    }
    binding.ivClose.setOnClickListener {
      dismiss()
    }
    BaseApp.handler.postDelayed({
      val rootPath = module.getCloudRootPath(storageType)
      getFileList(rootPath)
    }, 200)
  }

  /**
   * 获取文件列表
   */
  private fun getFileList(path: String) {
    showLoadView()
    val realPath = if (TextUtils.isEmpty(path)) module.getCloudRootPath(storageType) else path
    binding.path.text = realPath
    module.getFileList(storageType, realPath)
        .observe(this, Observer { list ->
          hintLoadView()
          if (list == null || list.isEmpty()) {
            binding.tempView.visibility = View.VISIBLE
            return@Observer
          } else {
            binding.tempView.visibility = View.GONE
          }
          curDirList.clear()
          curDirList.addAll(list)
          adapter.notifyDataSetChanged()
        })
  }

  private fun showLoadView() {
    binding.list.visibility = View.GONE
    binding.anim.visibility = View.VISIBLE
    binding.path.visibility = View.GONE
    try {
      binding.anim.setAnimation(
          requireContext().assets
              .open("loadingAnimation.json", AssetManager.ACCESS_STREAMING),
          "LottieCache"
      )
    } catch (e: IOException) {
      e.printStackTrace()
    }
  }

  private fun hintLoadView() {
    binding.list.visibility = View.VISIBLE
    binding.anim.cancelAnimation()
    binding.anim.visibility = View.GONE
    binding.path.visibility = View.VISIBLE
  }

  /**
   * 列表adapter
   */
  private class Adapter(
    context: Context,
    fileList: List<CloudFileInfo>
  ) : AbsRVAdapter<CloudFileInfo, Holder>(context, fileList) {

    private class Holder(view: View) : AbsHolder(view) {
      val icon: ImageView = view.findViewById(R.id.icon)
      val title: TextView = view.findViewById(R.id.title)
      val des: TextView = view.findViewById(R.id.des)
    }

    override fun getViewHolder(
      convertView: View,
      viewType: Int
    ): Holder {
      return Holder(convertView)
    }

    override fun setLayoutId(type: Int): Int {
      return R.layout.item_cloud_file_list
    }

    override fun bindData(
      holder: Holder,
      position: Int,
      item: CloudFileInfo
    ) {
      holder.title.text = item.fileName
      if (item.isDir) {
        holder.des.visibility = View.GONE
        holder.icon.setImageResource(R.drawable.ic_folder_24px)
        (holder.title.layoutParams as RelativeLayout.LayoutParams).addRule(CENTER_VERTICAL)
      } else {
        (holder.title.layoutParams as RelativeLayout.LayoutParams).removeRule(CENTER_VERTICAL)
        holder.des.visibility = View.VISIBLE
        holder.des.text = KeepassAUtil.instance.formatTime(item.serviceModifyDate)
        holder.icon.setImageResource(R.drawable.ic_file_24px)
      }
    }
  }

}