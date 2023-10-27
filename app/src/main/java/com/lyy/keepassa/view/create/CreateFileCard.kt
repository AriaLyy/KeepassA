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
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.arialyy.frame.router.Routerfit
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedBinary
import com.lyy.keepassa.R
import com.lyy.keepassa.databinding.LayoutEntryCreateStrCardBinding
import com.lyy.keepassa.router.DialogRouter
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

    fileAdapter.setOnItemClickListener { _, _, position ->
      val data = fileList[position]
      if (data == ADD_MORE_DATA) {
        Routerfit.create(DialogRouter::class.java).showCreateCustomDialog()
        return@setOnItemClickListener
      }
      (context as CreateEntryActivity).changeFile()
    }
    helper.handleList{
      TODO("未实现")
    }
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
        if (visibility == GONE){
          visibility = VISIBLE
        }
        fileList.remove(ADD_MORE_DATA)
        fileList.add(it)
        fileList.add(ADD_MORE_DATA)
        fileAdapter.notifyDataSetChanged()
      }
    }
  }

  private class FileAdapter :
    BaseQuickAdapter<Pair<String, ProtectedBinary>, BaseViewHolder>(R.layout.layout_entry_attachment) {
    override fun convert(holder: BaseViewHolder, item: Pair<String, ProtectedBinary>) {
      holder.setText(R.id.value, item.first)
    }
  }
}