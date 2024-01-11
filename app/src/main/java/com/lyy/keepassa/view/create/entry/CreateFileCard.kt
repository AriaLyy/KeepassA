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
import androidx.lifecycle.lifecycleScope
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedBinary
import com.lyy.keepassa.R
import com.lyy.keepassa.base.AbsViewBindingAdapter
import com.lyy.keepassa.databinding.LayoutEntryAttachmentBinding
import com.lyy.keepassa.databinding.LayoutEntryCreateStrCardBinding
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.doOnItemClickListener
import com.lyy.keepassa.util.init
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @Author laoyuyu
 * @Description
 * @Date 3:26 PM 2023/10/25
 **/
class CreateFileCard(context: Context, attributeSet: AttributeSet) :
  ConstraintLayout(context, attributeSet) {
  companion object {
    private val ADD_MORE_DATA = Pair("addMore", ProtectedBinary(false, null))
  }

  private val binding =
    LayoutEntryCreateStrCardBinding.inflate(LayoutInflater.from(context), this, true)

  val fileList = mutableListOf<Pair<String, ProtectedBinary>>()
  private val fileAdapter = FileAdapter()
  private val helper = CardListHelper(binding)

  init {
    listenerAddAttr()
    binding.tvTitle.text = ResUtil.getString(R.string.attachment)
  }

  fun bindData(entry: PwEntryV4) {
    fileList.clear()
    entry.binaries.entries.forEach {
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
            fileList.remove(data)
            fileAdapter.notifyItemRemoved(position)
            entry.binaries.remove(data.first)
            KpaUtil.kdbHandlerService.saveDbByBackground()
          }

          R.id.open_file -> {
            KdbUtil.openFile(data.first, data.second)
          }
        }
      }.show()
    }
    helper.handleList()
  }

  private fun listenerAddAttr() {
    if (isInEditMode) {
      return
    }
    (context as CreateEntryActivity).lifecycleScope.launch {
      CreateEntryModule.attrFlow.collectLatest {
        if (it == null) {
          return@collectLatest
        }
        if (visibility == GONE) {
          visibility = VISIBLE
        }
        if (fileList.isNotEmpty()) {
          fileList.removeLast()
        }
        fileList.add(it)
        fileList.add(ADD_MORE_DATA)
        fileAdapter.notifyDataSetChanged()
      }
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