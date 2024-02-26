/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.create.entry

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.security.ProtectedBinary
import com.lyy.keepassa.R
import com.lyy.keepassa.base.AbsViewBindingAdapter
import com.lyy.keepassa.databinding.LayoutEntryAttachmentBinding
import com.lyy.keepassa.databinding.LayoutEntryCreateStrCardBinding
import com.lyy.keepassa.entity.CommonState.DELETE
import com.lyy.keepassa.event.AttrFileEvent
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.doOnItemClickListener
import com.lyy.keepassa.util.init
import kotlinx.coroutines.launch

/**
 * @Author laoyuyu
 * @Description
 * @Date 3:26 PM 2023/10/25
 **/
class CreateFileCard(context: Context, attributeSet: AttributeSet) :
  ConstraintLayout(context, attributeSet) {
  companion object {
    val ADD_MORE_DATA = Pair("addMore", ProtectedBinary(false, null))
  }

  private val binding =
    LayoutEntryCreateStrCardBinding.inflate(LayoutInflater.from(context), this, true)

  private val fileList = mutableListOf<Pair<String, ProtectedBinary>>()
  private val fileAdapter = FileAdapter()
  private val helper = CardListHelper(binding)

  init {
    binding.tvTitle.text = ResUtil.getString(R.string.attachment)
  }

  fun bindData(fileMap: HashMap<String, ProtectedBinary>) {
    fileList.clear()
    fileMap.entries.forEach {
      fileList.add(Pair(it.key, it.value))
    }
    fileList.add(ADD_MORE_DATA)
    binding.rvList.apply {
      this.adapter = this@CreateFileCard.fileAdapter
      this@CreateFileCard.fileAdapter.setData(fileList)
    }
    binding.rvList.doOnItemClickListener { _, position, v ->
      val data = fileList[position]
      if (data == ADD_MORE_DATA) {
        (context as CreateEntryActivity).changeFile()
        return@doOnItemClickListener
      }
      PopupMenu(context, v, Gravity.END).init(R.menu.entry_modify_file_summary) {
        when (it.itemId) {
          R.id.remove_file -> {
            KpaUtil.scope.launch {
              CreateEntryModule.attrFlow.emit(AttrFileEvent(DELETE, data.first, data.second))
            }
          }

          R.id.open_file -> {
            KdbUtil.openFile(data.first, data.second)
          }
        }
      }.show()
    }
    helper.handleList()
  }

  fun removeItem(key: String) {
    fileList.find { it.first == key }?.let {
      val pos = fileList.indexOf(it)
      if (pos >= 0) {
        fileList.removeAt(pos)
        fileAdapter.notifyItemRemoved(pos)
      }
    }
    if (fileList.size == 1) {
      visibility = GONE
    }
  }

  private class FileAdapter :
    AbsViewBindingAdapter<Pair<String, ProtectedBinary>, LayoutEntryAttachmentBinding>() {

    override fun bindData(
      binding: LayoutEntryAttachmentBinding,
      item: Pair<String, ProtectedBinary>
    ) {
      if (item == ADD_MORE_DATA) {
        binding.value.isVisible = false
        binding.addMore.isVisible = true
        binding.addMore.text = ResUtil.getString(R.string.add_attr_file)
        return
      }
      binding.value.isVisible = true
      binding.addMore.isVisible = false
      binding.value.text = item.first
    }
  }
}