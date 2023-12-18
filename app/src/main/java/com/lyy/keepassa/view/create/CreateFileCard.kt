/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.create

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.arialyy.frame.util.ResUtil
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedBinary
import com.lyy.keepassa.R
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

  private val fileList = mutableListOf<Pair<String, ProtectedBinary>>()
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
      this@CreateFileCard.fileAdapter.setNewInstance(fileList)
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
        fileList.removeLast()
        fileList.add(it)
        fileList.add(ADD_MORE_DATA)
        fileAdapter.notifyDataSetChanged()
      }
    }
  }

  private class FileAdapter :
    BaseQuickAdapter<Pair<String, ProtectedBinary>, BaseViewHolder>(R.layout.layout_entry_attachment) {
    override fun convert(holder: BaseViewHolder, item: Pair<String, ProtectedBinary>) {
      if (item == ADD_MORE_DATA) {
        showContent(holder, false)
        return
      }
      showContent(holder, true)
      holder.setText(R.id.value, item.first)
    }

    private fun showContent(holder: BaseViewHolder, show: Boolean) {
      holder.setGone(R.id.value, !show)
      holder.setGone(R.id.add_more, show)
    }
  }
}