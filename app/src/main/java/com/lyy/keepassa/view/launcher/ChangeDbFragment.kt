/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.launcher

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.transition.TransitionInflater
import com.alibaba.android.arouter.facade.annotation.Route
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.adapter.AbsHolder
import com.arialyy.frame.util.adapter.AbsRVAdapter
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseFragment
import com.lyy.keepassa.databinding.FragmentChangeDbBinding
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.router.ActivityRouter
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.view.StorageType
import com.lyy.keepassa.view.StorageType.AFS
import com.lyy.keepassa.view.StorageType.DROPBOX
import com.lyy.keepassa.view.StorageType.GOOGLE_DRIVE
import com.lyy.keepassa.view.StorageType.ONE_DRIVE
import com.lyy.keepassa.view.StorageType.WEBDAV
import com.lyy.keepassa.view.launcher.ChangeDbFragment.Adataer.Holder

/**
 * 选择数据库
 */
@Route(path = "/launcher/changeDb")
class ChangeDbFragment : BaseFragment<FragmentChangeDbBinding>() {
  private lateinit var modlue: LauncherModule
  private var storageType: StorageType = AFS
  private lateinit var dbDelegate: IOpenDbDelegate

  companion object {
    val FM_TAG = "ChangeDbFragment"
  }

  override fun initData() {
    context?.let {
      enterTransition = TransitionInflater.from(it).inflateTransition(R.transition.slide_enter)
      exitTransition = TransitionInflater.from(it).inflateTransition(R.transition.slide_exit)
      returnTransition = TransitionInflater.from(it).inflateTransition(R.transition.slide_return)
    }

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
        if (KeepassAUtil.instance.isFastClick()
          || activity == null
          || position < 0
          || position >= data.size
        ) {
          return@setOnItemClickListener
        }
        val typeId = data[position].id
        if (this::dbDelegate.isInitialized) {
          lifecycle.removeObserver(dbDelegate)
        }
        when (typeId) {
          // 文件系统选择db
          AFS.type -> {
            storageType = AFS
            dbDelegate = OpenAFSDelegate()
          }
          DROPBOX.type -> {
            storageType = DROPBOX
            dbDelegate = OpenDropBoxDelegate()
          }
          WEBDAV.type -> {
            storageType = WEBDAV
            dbDelegate = OpenWebDavDelegate()
          }
          ONE_DRIVE.type -> {
            storageType = ONE_DRIVE
            dbDelegate = OpenOneDriveDelegate()
          }
          GOOGLE_DRIVE.type ->{
            storageType = GOOGLE_DRIVE
            dbDelegate = OpenGoogleDriveDelegate()
          }
          LauncherModule.HISTORY_ID -> { // 历史记录
            startActivity(
              Intent(context, OpenDbHistoryActivity::class.java),
              ActivityOptions.makeSceneTransitionAnimation(activity)
                .toBundle()
            )
          }
        }

        if (typeId != LauncherModule.HISTORY_ID) {
          dbDelegate.startFlow(this)
          lifecycle.addObserver(dbDelegate)
        }
      }
    binding.fab.setOnClickListener {
      activity?.let { ac ->
        Routerfit.create(ActivityRouter::class.java, ac).toCreateDbActivity(
          ActivityOptionsCompat.makeSceneTransitionAnimation(ac)
        )
      }
    }
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    if (this::dbDelegate.isInitialized) {
      dbDelegate.onActivityResult(requestCode, resultCode, data)
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