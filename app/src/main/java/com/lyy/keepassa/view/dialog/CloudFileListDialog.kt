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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.util.ResUtil
import com.arialyy.frame.util.adapter.AbsHolder
import com.arialyy.frame.util.adapter.AbsRVAdapter
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogCloudFileListBinding
import com.lyy.keepassa.event.ChangeDbEvent
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.cloud.CloudFileInfo
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.cloud.WebDavUtil
import com.lyy.keepassa.util.doOnItemClickListener
import com.lyy.keepassa.view.StorageType
import com.lyy.keepassa.view.StorageType.UNKNOWN
import com.lyy.keepassa.view.StorageType.WEBDAV
import com.lyy.keepassa.view.dialog.CloudFileListDialog.Adapter.Holder
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.IOException
import java.util.Stack

/**
 * 云文件列表
 */
@Route(path = "/dialog/cloudFileList")
class CloudFileListDialog : BaseDialog<DialogCloudFileListBinding>() {

  private val curDirList = ArrayList<CloudFileInfo>()
  private lateinit var adapter: Adapter
  private lateinit var module: CloudFileListModule
  private val pathStack = Stack<String>()
  private var lastPath: String = ""

  @Autowired(name = "storageType")
  @JvmField
  var storageType: StorageType = UNKNOWN

  override fun setLayoutId(): Int {
    return R.layout.dialog_cloud_file_list
  }

  override fun initData() {
    super.initData()
    ARouter.getInstance().inject(this)
    module = ViewModelProvider(this)[CloudFileListModule::class.java]
    adapter = Adapter(requireContext(), curDirList)
    binding.list.adapter = adapter
    binding.list.setHasFixedSize(true)
    binding.list.layoutManager = LinearLayoutManager(context)

    binding.list.doOnItemClickListener { _, position, _ ->
      if (position == 0) {
        if (pathStack.isEmpty()) {
          Timber.d(ResUtil.getString(R.string.error_is_root))
          return@doOnItemClickListener
        }
        lastPath = pathStack.pop()
        getFileList(lastPath)
        return@doOnItemClickListener
      }

      val item = curDirList[position]
      if (item.isDir) {
        pathStack.push(lastPath)
        lastPath = item.fileKey
        getFileList(item.fileKey)
        return@doOnItemClickListener
      }
      lifecycleScope.launch {
        val cloudPath =
          if (storageType == WEBDAV) "${WebDavUtil.getHostUri()}${item.fileKey}" else item.fileKey
        Timber.d("couldPath = $cloudPath")

        if (storageType == WEBDAV) {
          module.saveWebHistory(cloudPath)
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
              cloudPath = cloudPath,
              uriType = storageType
            )
          )
        dismiss()
      }
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
        curDirList.clear()
        curDirList.add(0, module.upEntry)
        if (!list.isNullOrEmpty()) {
          curDirList.addAll(list)
        }
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