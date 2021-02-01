/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.widget.editor

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.inputmethod.InputMethod
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.widget.ContentLoadingProgressBar
import com.example.uiwidget.R
import com.zzhoujay.richtext.RichText

/**
 * @Author laoyuyu
 * @Description
 * @Date 2020/11/30
 **/
class MarkDownEditor(
  context: Context,
  attributeSet: AttributeSet
) : RelativeLayout(context, attributeSet), OnClickListener {
  private lateinit var editor: EditorView
  private lateinit var preBtn: ImageView
  private lateinit var preText: TextView
  private lateinit var pb: ContentLoadingProgressBar
  private lateinit var saveBtn: View
  private lateinit var redoBtn: View
  private lateinit var undoBtn: View
  private lateinit var clearBtn: View
  private var saveListener: OnSaveListener? = null

  interface OnSaveListener {
    fun onSave(content: CharSequence?)
  }

  init {
    LayoutInflater.from(context)
        .inflate(R.layout.layout_markdown_editor, this, true)
  }

  override fun onFinishInflate() {
    super.onFinishInflate()
    saveBtn = findViewById(R.id.ivSave)
    saveBtn.setOnClickListener(this)
    clearBtn = findViewById(R.id.ivClear)
    clearBtn.setOnClickListener(this)
    redoBtn = findViewById(R.id.ivRedo)
    redoBtn.setOnClickListener(this)
    undoBtn = findViewById(R.id.ivUndo)
    undoBtn.setOnClickListener(this)
    findViewById<View>(R.id.tvH).setOnClickListener(this)
    findViewById<View>(R.id.tvItemLess).setOnClickListener(this)
    findViewById<View>(R.id.tvItem).setOnClickListener(this)
    findViewById<View>(R.id.tvBlock).setOnClickListener(this)
    findViewById<View>(R.id.ivTable).setOnClickListener(this)
    findViewById<View>(R.id.ivTodo).setOnClickListener(this)
    findViewById<View>(R.id.ivImg).setOnClickListener(this)
    findViewById<View>(R.id.tvBold).setOnClickListener(this)
    preBtn = findViewById(R.id.ivPre)
    preText = findViewById(R.id.tvPre)
    pb = findViewById(R.id.pb)
    preBtn.setOnClickListener(this)
    editor = findViewById(R.id.editor)
  }

  fun setOnSaveListener(listener: OnSaveListener) {
    this.saveListener = listener
  }

  fun setText(content: CharSequence?) {
    content?.let {
      editor.setText(content)
    }
  }

  /**
   * 是否显示顶部栏工具
   */
  private fun showTopBarTool(show: Boolean) {
    if (show) {
      saveBtn.visibility = VISIBLE
      redoBtn.visibility = VISIBLE
      undoBtn.visibility = VISIBLE
      clearBtn.visibility = GONE
      preText.visibility = View.GONE
    } else {
      saveBtn.visibility = GONE
      redoBtn.visibility = GONE
      undoBtn.visibility = GONE
      clearBtn.visibility = GONE
      preText.visibility = View.VISIBLE
    }
  }

  override fun onClick(v: View?) {
    when (v?.id) {
      R.id.ivPre -> { // 预览
        if (preBtn.isSelected) {
          // 从预览回到编辑
          showTopBarTool(true)
        } else {
          val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
          imm?.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
          // 预览
          pre()
        }
        preBtn.isSelected = !preBtn.isSelected
      }
      R.id.ivSave -> { // 保存
        saveListener?.onSave(if (editor.text.isNullOrEmpty()) null else editor.text.toString())
      }
      R.id.ivClear -> { // 清空
        editor.clear()
      }
      R.id.ivUndo -> { // 撤销
        editor.undo()
      }
      R.id.ivRedo -> { // 回退
        editor.redo()
      }
      R.id.tvBold -> { // bold
        input("****", false)
        editor.setSelection(editor.selectionStart - 2)
      }
      R.id.tvH -> { // #
        input("# ", false)
      }
      R.id.tvItemLess -> { // -
        input("- ", false)
      }
      R.id.tvItem -> { // *
        input("* ", false)
      }
      R.id.tvBlock -> { // >
        input(">", false)
      }
      R.id.ivTable -> { // 表格
        val tableStr = "|   A   |   B   |   C   |\n| ---- | ---- | ---- |\n|      |      |      |"
        input(tableStr)
      }
      R.id.ivTodo -> { // 清单
        input("- []")
      }
      R.id.ivImg -> { // 图片
        input("[]()")
        editor.setSelection(editor.selectionStart - 4)
      }
    }
  }

  /**
   * 预览
   */
  private fun pre() {
    pb.show()
    showTopBarTool(false)
    RichText.initCacheDir(context)
    RichText.fromMarkdown(editor.text.toString())
        .done {
          pb.hide()
        }
        .into(preText)
  }

  private fun input(
    str: CharSequence,
    afterEnter: Boolean = true
  ) {
    val temp = if (afterEnter) "\n" else ""
    editor.text?.insert(editor.selectionStart, "\n$str$temp")
    editor.addOperateStr(str.toString(), editor.selectionStart)
  }

}