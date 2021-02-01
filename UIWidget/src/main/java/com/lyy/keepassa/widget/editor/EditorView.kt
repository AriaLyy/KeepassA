/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.widget.editor

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatEditText
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive

/**
 * @Author laoyuyu
 * @Description editor
 * @Date 2020/11/30
 **/
class EditorView(
  context: Context,
  attributeSet: AttributeSet
) : AppCompatEditText(context, attributeSet) {
  private val TAG = javaClass.simpleName
  private var scope = MainScope()
  private val INTERVAL = 1000L
  private var lastOperateTime = System.currentTimeMillis()
  private var isOperating = false

  private val operateManager by lazy {
    OperateManager(this.editableText)
  }

  private val cache by lazy {
    CharBuffer(this.editableText)
  }

  override fun onFinishInflate() {
    super.onFinishInflate()
    // 关闭拼写检查，防止onTextChanged错乱问题
    inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_FLAG_MULTI_LINE
    isSingleLine = false
    addTextChangedListener(object : TextWatcher {
      var beforeStr = ""
      override fun beforeTextChanged(
        s: CharSequence?,
        start: Int,
        count: Int,
        after: Int
      ) {
        s?.let { beforeStr = it.toString() }
      }

      /**
       * s: 改变后的字符串
       * start: 有变动的字符串的序号
       * before: 被改变的字符串长度，如果是新增则为 0。
       * count: 添加的字符串长度，如果是删除则为 0。
       */
      override fun onTextChanged(
        s: CharSequence?,
        start: Int,
        before: Int,
        count: Int
      ) {
        Log.d(TAG, "onTextChanged: s = $s, start = $start, before = $before, count = $count")
        if (isOperating) {
          Log.w(TAG, "isOperating")
          return
        }
        /*
         delete str
        */
        if (count == 0) {
          // before del str need add str form cache, and clear cache
//          addNewStr(null)
          val delStr = if (s.isNullOrEmpty()) {
            beforeStr
          } else {
            beforeStr.substring(start - before + 1, start + 1)
          }
          Log.d(TAG, "beforeStr = ${beforeStr}, delete Str = $delStr")
          operateManager.delete(delStr, start - before, start)
        }

        if (s.isNullOrEmpty()) return
        // add new str
        if (before == 0 && count != 0) {
          val newStr = s.subSequence(start, start + count)
          addNewStr(newStr.toString(), start)

//          if (System.currentTimeMillis() - lastOperateTime > INTERVAL) {
//            addNewStr(newStr.toString())
//            return
//          }
//          sb.append(newStr)
          return
        }

      }

      override fun afterTextChanged(s: Editable?) {

      }

    })
  }

  override fun onTextChanged(
    text: CharSequence?,
    start: Int,
    lengthBefore: Int,
    lengthAfter: Int
  ) {
    super.onTextChanged(text, start, lengthBefore, lengthAfter)
    isOperating = false
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (!scope.isActive) {
      scope = MainScope()
    }
//    scope.launch(Dispatchers.IO) {
//      repeat(Int.MAX_VALUE) {
//        delay(INTERVAL * 2)
//        if (!isAdding) {
////          Log.i(TAG, "repeat add new Str = $sb")
//          addNewStr(null)
//        }
//      }
//    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    operateManager.destroy()
    scope.cancel()
  }

  /**
   * add new str to container
   */
  private fun addNewStr(
    s: String,
    start: Int
  ) {
    lastOperateTime = System.currentTimeMillis()
    operateManager.add(s, start)
  }

  fun addOperateStr(
    s: String,
    start: Int
  ) {
    operateManager.add(s, start)
  }

  fun undo() {
    checkCache()
    isOperating = true
    operateManager.undo()
    isOperating = false
  }

  fun redo() {
    checkCache()
    isOperating = true
    operateManager.redo()
    isOperating = false
  }

  fun clear() {
    checkCache()
    isOperating = true
    val newStr = operateManager.clear()
    setText(newStr)
    setSelection(newStr.length)
  }

  /**
   * if the cache has some char, before operate, need addCache str
   */
  private fun checkCache() {
    if (cache.isEmpty()) {
      return
    }
    val p = cache.getCache()

    addNewStr(p.second.toString(), p.first)
  }

  private class CharBuffer(val ed: Editable) {
    private var sb = StringBuilder()
    private var start: Int = ed.length
    private fun addChart(
      start: Int,
      charSequence: CharSequence
    ) {
      this.start = start
      sb.append(charSequence)
    }

    fun isEmpty(): Boolean {
      return sb.isEmpty()
    }

    /**
     * get cahce and reset start
     */
    fun getCache(): Pair<Int, CharSequence> {
      val str = sb.toString()
      val p = Pair(start, str)
      start = ed.length
      sb.clear()
      return p
    }
  }

}