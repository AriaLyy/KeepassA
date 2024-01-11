/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.view.create.entry

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.arialyy.frame.router.Routerfit
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.base.AbsViewBindingAdapter
import com.lyy.keepassa.databinding.LayoutEntryCreateStrCardBinding
import com.lyy.keepassa.databinding.LayoutEntryStrBinding
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.doOnItemClickListener
import com.lyy.keepassa.util.init
import com.lyy.keepassa.view.create.CreateCustomStrDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 3:12 PM 2023/10/12
 **/
class CreateStrCard(context: Context, attributeSet: AttributeSet) :
  ConstraintLayout(context, attributeSet) {
  private val binding =
    LayoutEntryCreateStrCardBinding.inflate(LayoutInflater.from(context), this, true)
  val strList = mutableListOf<Pair<String, ProtectedString>>()

  companion object {
    private val ADD_MORE_DATA = Pair("addMore", ProtectedString(false, "addMore"))
  }

  private val helper = CardListHelper(binding)

  init {
    listenerModifyStr()
  }

  private val adapter = StrAdapter()

  fun bindDate(entry: PwEntryV4) {
    handleList(entry)
  }

  private fun handleList(entry: PwEntryV4) {
    strList.clear()
    KdbUtil.filterCustomStr(entry).entries.forEach {
      strList.add(Pair(it.key, it.value))
    }
    strList.add(ADD_MORE_DATA)
    binding.rvList.adapter = adapter
    adapter.setData(strList)

    binding.rvList.doOnItemClickListener { _, position, v ->
      val data = strList[position]
      if (data == ADD_MORE_DATA) {
        Routerfit.create(DialogRouter::class.java).showCreateCustomDialog()
        return@doOnItemClickListener
      }
      PopupMenu(context, v, Gravity.END).init(R.menu.entry_modify_str_summary) {
        when (it.itemId) {
          R.id.remove_str -> {
            strList.remove(data)
            adapter.notifyItemRemoved(position)
            entry.binaries.remove(data.first)
            KpaUtil.kdbHandlerService.saveDbByBackground()
          }

          R.id.modify_text -> {
            Routerfit.create(DialogRouter::class.java)
              .showCreateCustomDialog(position, data.first, data.second)
          }
        }
      }.show()
    }
    helper.handleList()
  }

  private fun listenerModifyStr() {
    if (isInEditMode) {
      return
    }
    (context as CreateEntryActivity).lifecycleScope.launch {
      CreateCustomStrDialog.createCustomStrFlow.collectLatest { str ->
        if (str == null) {
          Timber.d("attr is null")
          return@collectLatest
        }
        if (visibility == GONE) {
          visibility = VISIBLE
        }
        if (str.isEdit) {
          strList[str.position] = Pair(str.key, str.str)
          return@collectLatest
        }
        if (strList.isNotEmpty()) {
          strList.removeLast()
        }
        strList.add(Pair(str.key, str.str))
        strList.add(ADD_MORE_DATA)
        adapter.notifyDataSetChanged()
      }
    }
  }

  private class StrAdapter :
    AbsViewBindingAdapter<Pair<String, ProtectedString>, LayoutEntryStrBinding>() {

    private fun showContent(binding: LayoutEntryStrBinding, show: Boolean) {
      binding.title.isGone = !show
      binding.value.isGone = !show
      binding.addMore.isGone = show
    }

    override fun bindData(binding: LayoutEntryStrBinding, item: Pair<String, ProtectedString>) {
      if (item == ADD_MORE_DATA) {
        showContent(binding, false)
        return
      }
      showContent(binding, true)
      binding.title.text = item.first

      val tvValue = binding.value
      KpaUtil.handleShowPass(tvValue, !item.second.isProtected)
      if (item.second.toString().isEmpty()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          tvValue.typeface = context.resources.getFont(R.font.roboto_thinitalic)
        }
        tvValue.text = "null"
        return
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        tvValue.typeface = context.resources.getFont(R.font.roboto_regular)
      }
      tvValue.text = item.second.toString()
    }
  }
}