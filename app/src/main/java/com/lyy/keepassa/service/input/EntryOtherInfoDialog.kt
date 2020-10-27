/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.input

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.databinding.DialogOtherInfoBinding
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.event.FillInfoEvent
import com.lyy.keepassa.util.HitUtil
import org.greenrobot.eventbus.EventBus
import java.util.UUID

/**
 * @Author laoyuyu
 * @Description 其它信息
 * @Date 2020/10/25
 **/
class EntryOtherInfoDialog : BaseActivity<DialogOtherInfoBinding>() {
  companion object {
    val KEY_DATA = "KEY_DATA"
  }

  private var entryUUID: UUID? = null
  private val moreInfoList = arrayListOf<SimpleItemEntity>()
  private val moreInfoAdapter by lazy {
    EntryOtherInfoAdapter(this, moreInfoList)
  }
  private lateinit var pwEntry: PwEntryV4

  override fun setLayoutId(): Int {
    return R.layout.dialog_other_info
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    entryUUID = intent.getSerializableExtra(KEY_DATA) as UUID?
    if (entryUUID == null) {
      HitUtil.toaskShort(getString(R.string.hint_not_nore_info))
      finish()
      return
    }
    pwEntry = BaseApp.KDB.pm.entries[entryUUID] as PwEntryV4
    binding.rvList.adapter = moreInfoAdapter
    binding.rvList.setHasFixedSize(true)
    binding.rvList.layoutManager = LinearLayoutManager(this)
    RvItemClickSupport.addTo(binding.rvList)
        .setOnItemClickListener { _, position, _ ->
          EventBus.getDefault()
              .post(FillInfoEvent(moreInfoList[position].content))
          finish()
        }
    setEntry(pwEntry)
  }

  private fun setEntry(pwEntry: PwEntryV4?) {
    if (pwEntry == null) {
      HitUtil.toaskShort(getString(R.string.hint_not_nore_info))
      finish()
      return
    }
    val list = getStrList(pwEntry)
    if (list.isNullOrEmpty()) {
      HitUtil.toaskShort(getString(R.string.hint_not_nore_info))
      finish()
      return
    }
    moreInfoList.clear()
    moreInfoList.addAll(list)
    moreInfoAdapter.notifyDataSetChanged()
  }

  private fun getStrList(pwEntry: PwEntryV4): ArrayList<SimpleItemEntity> {
    val list = arrayListOf<SimpleItemEntity>()
    if (!pwEntry.url.isNullOrEmpty()) {
      val url = SimpleItemEntity()
      url.title = getString(R.string.url)
      url.content = pwEntry.url
      url.isProtected = false
      list.add(url)
    }

    if (!pwEntry.notes.isNullOrEmpty()) {
      val notes = SimpleItemEntity()
      notes.title = getString(R.string.notice)
      notes.content = pwEntry.notes
      notes.isProtected = false
      list.add(notes)
    }

    if (pwEntry.strings.isNotEmpty()) {
      pwEntry.strings.forEach {
        if (it.value.toString().isEmpty()
            || it.key.equals(PwEntryV4.STR_TITLE, true)
            || it.key.equals(PwEntryV4.STR_USERNAME, true)
            || it.key.equals(PwEntryV4.STR_PASSWORD, true)){
          return@forEach
        }
        val item = SimpleItemEntity()
        item.title = it.key
        item.content = it.value.toString()
        item.isSelected = it.value.isProtected
        item.isProtected = it.value.isProtected
        list.add(item)
      }
    }
    return list
  }
}