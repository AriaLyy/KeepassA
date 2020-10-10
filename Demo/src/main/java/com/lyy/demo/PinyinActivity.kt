/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.demo

import android.os.Bundle
import android.util.Log
import android.view.View
import com.arialyy.frame.core.AbsActivity
import com.arialyy.frame.util.PinyinUtil
import com.lyy.demo.databinding.ActivityPinyinBinding
import java.lang.StringBuilder
import java.util.Arrays

class PinyinActivity : AbsActivity<ActivityPinyinBinding>() {
  private val sortChart = arrayListOf(
      "去年",
      "玩家",
      "饿了么",
      "日天",
      "阴阳",
      "哦",
      "烹制",
      "三年",
      "导洞",
      "房间家",
      "工资没了",
      "韩国棒子",
      "金",
      "开飞车",
      "龙部落",
      "下限",
      "批亦能",
      "你好",
      "啊",
      "笔帽子",
      "中国",
      "天朝",
      "萌妹子",
      "脑残",
      "1",
      "3",
      "444",
      "222",
      "00",
      "88",
      "9",
      "765432d1",
      "666",
      "5982",
      "qwe",
      "w",
      "e",
      "r",
      "t",
      "y",
      "u",
      "iiiiiii",
      "o",
      "pqwee",
      "asd",
      "sd",
      "dfs",
      "g",
      "hhj",
      "jj",
      "k",
      "l",
      "z",
      "xc",
      "cv",
      "vb",
      "bn",
      "mn"
  )

  override fun setLayoutId(): Int {
    return R.layout.activity_pinyin
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    binding.et.setText("拼音123hhhe")
    binding.sortChar.text = Arrays.toString(sortChart.toArray())
    sort()
  }

  private fun sort() {
    val map = hashMapOf<String, Char?>()
    for (str in sortChart) {
      map[str] = PinyinUtil.getFirstSpellChar(str)
    }
    for (m in map) {
      Log.d(TAG, m.toString())
    }

    val asc = map.toList().sortedBy { it.second }.toMap()

    for (m in asc) {
      Log.i(TAG, m.toString())
    }
    val sb = StringBuilder()
    sb.append("升序：\n").append(asc.keys.toTypedArray().contentToString()).append("\n")
    sb.append("降序：\n")
    val desc = map.toList().sortedByDescending { it.second }.toMap()
    sb.append(desc.keys.toTypedArray().contentToString())
    binding.sortResult.text = sb.toString()
  }

  fun onClick(View: View?) {
    binding.hint.text = PinyinUtil.getSpells(binding.et.text.toString())
  }

}