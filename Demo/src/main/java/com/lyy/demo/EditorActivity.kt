/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.demo

import android.os.Bundle
import android.view.View
import com.arialyy.frame.core.AbsActivity
import com.lyy.demo.databinding.ActivityEditorBinding

/**
 * @Author laoyuyu
 * @Description
 * @Date 2020/11/30
 **/
class EditorActivity : AbsActivity<ActivityEditorBinding>() {

  override fun setLayoutId(): Int {
    return R.layout.activity_editor
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)

  }

  fun onClick(v: View) {
    when (v.id) {
      R.id.undo -> {
        binding.et.undo()
      }
      R.id.redo -> {
        binding.et.redo()
      }
      R.id.clear -> {
        binding.et.clear()
      }
    }
  }

}