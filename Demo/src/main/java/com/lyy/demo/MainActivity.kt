/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.demo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arialyy.frame.core.AbsActivity
import com.arialyy.frame.util.adapter.AbsHolder
import com.arialyy.frame.util.adapter.AbsRVAdapter
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.lyy.demo.MainActivity.Adapter.Holder
import com.lyy.demo.databinding.ActivityMainBinding

class MainActivity : AbsActivity<ActivityMainBinding>() {

  private val strings = mutableListOf("uri权限管理", "指纹加密数据", "指纹校验", "富文本", "注册码", "捐赠", "首字母", "展开文本")

  override fun setLayoutId(): Int {
    return R.layout.activity_main
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    val adapter = Adapter(this, strings)
    val list = findViewById<RecyclerView>(R.id.list)
    list.adapter = adapter
    list.layoutManager = LinearLayoutManager(this)
    list.setHasFixedSize(true)
    adapter.notifyDataSetChanged()
    RvItemClickSupport.addTo(list)
        .setOnItemClickListener { _, position, _ ->
          when (position) {
            0 -> { // uri权限管理
              startActivity(Intent(this, UriActivity::class.java))
            }
            1 -> { // 指纹加密数据
              startActivity(Intent(this, FingerprintActivity::class.java))
            }
            2 -> { // 指纹校验
              startActivity(Intent(this, FingerprintVerifyActivity::class.java))
            }
            3 -> { // 富文本
              startActivity(Intent(this, RichTextActivity::class.java))
            }
            4 -> { // 注册码
              startActivity(Intent(this, RegCodeActivity::class.java))
            }
            5 -> { // 捐赠
              startActivity(Intent(this, DonateActivity::class.java))
            }
            6 -> { //拼音首字母
              startActivity(Intent(this, PinyinActivity::class.java))
            }
            7 -> {
              startActivity(Intent(this, ExpendTextViewActivity::class.java))
            }
          }
        }
  }

  override fun onRestart() {
    super.onRestart()
    Log.d(TAG, "A onRestart")
  }

  override fun onStart() {
    super.onStart()
    Log.d(TAG, "A onStart")
  }

  override fun onResume() {
    super.onResume()
    Log.d(TAG, "A onResume")
  }

  override fun onPause() {
    super.onPause()
    Log.d(TAG, "A onPause")
  }

  override fun onStop() {
    super.onStop()
    Log.d(TAG, "A onStop")
  }

  override fun onDestroy() {
    super.onDestroy()
    Log.d(TAG, "A onDestroy")
  }

  private class Adapter(
    context: Context,
    datas: List<String>
  ) : AbsRVAdapter<String, Holder>(context, datas) {

    private class Holder(view: View) : AbsHolder(view) {
      val tv: TextView = view.findViewById(R.id.tv)
    }

    override fun getViewHolder(
      convertView: View,
      viewType: Int
    ): Holder {
      return Holder(convertView)
    }

    override fun setLayoutId(type: Int): Int {
      return R.layout.item_funcation
    }

    override fun bindData(
      holder: Holder,
      position: Int,
      item: String
    ) {
      holder.tv.text = item
    }

  }

}