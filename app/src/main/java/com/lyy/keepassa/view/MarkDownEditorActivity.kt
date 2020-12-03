/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.databinding.ActivityMarkdownEditorBinding
import com.lyy.keepassa.event.EditorEvent
import com.lyy.keepassa.util.KLog
import com.lyy.keepassa.widget.editor.MarkDownEditor
import org.greenrobot.eventbus.EventBus

/**
 * @Author laoyuyu
 * @Description
 * @Date 2020/12/2
 **/
class MarkDownEditorActivity : BaseActivity<ActivityMarkdownEditorBinding>() {

  private var reqCode: Int = 0
  private var content: CharSequence? = null

  companion object {
    private val KEY_REQUESTOIN_CODE = "KEY_REQUESTOIN_CODE"
    private var KEY_CONTENT = "KEY_CONTENT"

    fun turnMarkDownEditor(
      context: Context,
      requestCode: Int,
      content: CharSequence?
    ) {
      val intent = Intent(context, MarkDownEditorActivity::class.java)
      intent.putExtra(KEY_REQUESTOIN_CODE, requestCode)
      intent.putExtra(KEY_CONTENT, content)
      if (context is Activity) {
        context.startActivity(
            intent,
            ActivityOptions.makeSceneTransitionAnimation(context)
                .toBundle()
        )
        return
      }
      context.startActivity(intent)
    }
  }

  override fun setLayoutId(): Int {
    return R.layout.activity_markdown_editor
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    reqCode = intent.getIntExtra(KEY_REQUESTOIN_CODE, -1)
    content = intent.getCharSequenceExtra(KEY_CONTENT)
    if (reqCode == -1) {
      KLog.e(TAG, "没有设置请求码")
      finishAfterTransition()
      return
    }

    binding.mdeEditor.setText(content)
    binding.mdeEditor.setOnSaveListener(object : MarkDownEditor.OnSaveListener {
      override fun onSave(content: CharSequence?) {
        EventBus.getDefault()
            .post(EditorEvent(reqCode, content))
        finishAfterTransition()
      }
    })
  }

}