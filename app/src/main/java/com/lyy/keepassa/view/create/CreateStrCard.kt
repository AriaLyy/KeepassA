/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.view.create

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.arialyy.frame.router.Routerfit
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.databinding.LayoutEntryCreateStrCardBinding
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KpaUtil
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
  private val strList = mutableListOf<Pair<String, ProtectedString>>()

  companion object {
    private val ADD_MORE_DATA = Pair("addMore", ProtectedString(false, "addMore"))
  }

  init {
    listenerModifyStr()
  }

  fun bindDate(entry: PwEntryV4) {
    handleList(entry)
  }

  private fun handleList(entry: PwEntryV4) {
    val adapter = StrAdapter()
    KdbUtil.filterCustomStr(entry).entries.forEach {
      strList.add(Pair(it.key, it.value))
    }
    strList.add(ADD_MORE_DATA)
    binding.rvList.apply {
      this.adapter = adapter
      setHasFixedSize(true)
      layoutManager = object : LinearLayoutManager(context) {
        override fun canScrollVertically(): Boolean {
          return false
        }
      }
      adapter.setNewInstance(strList)
      isNestedScrollingEnabled = false
    }
    adapter.setOnItemClickListener { _, _, position ->
      val data = strList[position]
      if (data == ADD_MORE_DATA) {
        Routerfit.create(DialogRouter::class.java).showCreateCustomDialog()
        return@setOnItemClickListener
      }
      Routerfit.create(DialogRouter::class.java)
        .showCreateCustomDialog(position, data.first, data.second)
      adapter.notifyDataSetChanged()
    }
    moveDrop()
  }

  private fun listenerModifyStr() {
    KpaUtil.scope.launch {
      CreateCustomStrDialog.createCustomStrFlow.collectLatest { str ->
        if (str == null) {
          Timber.d("attr is null")
          return@collectLatest
        }
        if (str.isEdit) {
          strList[str.position] = Pair(str.key, str.str)
          return@collectLatest
        }
        strList.removeLast()
        strList.add(Pair(str.key, str.str))
        strList.add(ADD_MORE_DATA)
      }
    }
  }

  /**
   * 滑动删除
   */
  private fun moveDrop() {
    ItemTouchHelper(object : ItemTouchHelper.Callback() {
      private var swipeThreshold = -1f
      override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN //允许上下的拖动
        val swipeFlags = ItemTouchHelper.LEFT //只允许从右向左侧滑
        return makeMovementFlags(dragFlags, swipeFlags)
      }

      override fun isLongPressDragEnabled(): Boolean {
        // 禁止拖动
        return false
      }

      override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: ViewHolder,
        target: ViewHolder
      ): Boolean {
        Timber.d("onMove")
        return true
      }

      override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
        val delBtn = viewHolder.itemView.findViewById<TextView>(R.id.del)
        viewHolder.itemView.translationX = -(delBtn.measuredWidth.toFloat())
      }

      override fun getSwipeThreshold(viewHolder: ViewHolder): Float {
        if (swipeThreshold < 0) {
          val delBtn = viewHolder.itemView.findViewById<TextView>(R.id.del)
          swipeThreshold = delBtn.measuredWidth.toFloat() / viewHolder.itemView.measuredWidth
        }
        return swipeThreshold
      }
    }).attachToRecyclerView(binding.rvList)
  }

  private class StrAdapter :
    BaseQuickAdapter<Pair<String, ProtectedString>, BaseViewHolder>(R.layout.layout_entry_str) {
    @SuppressLint("SetTextI18n")
    override fun convert(holder: BaseViewHolder, item: Pair<String, ProtectedString>) {
      val group = holder.getView<Group>(R.id.groupContent)
      val addMoreTv = holder.getView<TextView>(R.id.add_more)
      if (item == ADD_MORE_DATA) {
        group.visibility = GONE
        addMoreTv.visibility = VISIBLE
        return
      }
      group.visibility = VISIBLE
      addMoreTv.visibility = GONE
      holder.setText(R.id.title, item.first)
      val tvValue = holder.getView<TextView>(R.id.value)
      KpaUtil.handleShowPass(tvValue, !item.second.isProtected)
      holder.getView<View>(R.id.rpbBar).visibility = GONE
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