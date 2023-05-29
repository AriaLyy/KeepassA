/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util

import android.app.Activity
import android.view.MotionEvent
import android.view.View
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.Constance
import timber.log.Timber

val charRegex = Regex("[^a-zA-Z0-9]")

enum class ClickScope {
  /**
   * 仅在该View中有效
   */
  VIEW,

  /**
   * 全局的
   */
  SYS
}

private var lastClickTime = -1L

/**
 * 时间间隔
 * @param clickScope  时间间隔，[ClickScope.VIEW]，[ClickScope.SYS]
 */
fun View.doClick(
  intervalTime: Long = 1000,
  clickScope: ClickScope = ClickScope.VIEW,
  body: (View) -> Unit
) {
  var curTime = -1L
  fun viewScopeClick(it: View) {
    if (curTime == -1L || kotlin.math.abs(System.currentTimeMillis() - curTime) > intervalTime) {
      curTime = System.currentTimeMillis()
      body.invoke(it)
      return
    }
    Timber.d("间隔太短")
  }

  fun sysScopeClick(it: View) {
    if (kotlin.math.abs(System.currentTimeMillis() - lastClickTime) > intervalTime) {
      lastClickTime = System.currentTimeMillis()
      body.invoke(it)
      return
    }
    Timber.d("间隔太短")
  }

  setOnClickListener {
    if (intervalTime == 0L) {
      body.invoke(it)
      return@setOnClickListener
    }

    if (clickScope == ClickScope.VIEW) {
      viewScopeClick(it)
      return@setOnClickListener
    }

    if (clickScope == ClickScope.SYS) {
      sysScopeClick(it)
      return@setOnClickListener
    }

    Timber.d("间隔太短")
  }
}

fun Activity.isDestroy() = isDestroyed || isFinishing

/**
 * @return true has special char
 */
fun CharSequence.hasSpecialChar(): Boolean {
  return charRegex.containsMatchIn(this)
}

// fun CharSequence.specialEncode():CharSequence{
//   val out: StringBuilder = StringBuilder(length)
//   forEach {
//     it.
//   }
// }

/**
 * isOpenQuickLock
 * @return true already open quick lock
 */
fun BaseApp.isCanOpenQuickLock(): Boolean {
  return PreferenceManager.getDefaultSharedPreferences(this)
    .getBoolean(applicationContext.getString(R.string.set_quick_unlock), false)
    && BaseApp.dbRecord != null
    && !KpaUtil.isEmptyPass()
}

fun PwEntryV4.hasNote(): Boolean {
  for (str in this.strings) {
    if (str.key.equals(PwEntryV4.STR_NOTES, true)) {
      return true
    }
  }
  return false
}

fun PwEntryV4.hasTOTP(): Boolean {
  for (str in this.strings) {
    if (str.key.equals(PwEntryV4.STR_NOTES, true)
      || str.key.equals(PwEntryV4.STR_PASSWORD, true)
      || str.key.equals(PwEntryV4.STR_TITLE, true)
      || str.key.equals(PwEntryV4.STR_URL, true)
      || str.key.equals(PwEntryV4.STR_USERNAME, true)
    ) {
      continue
    }

    // 增加TOP密码字段
    if (str.key.startsWith("TOTP", ignoreCase = true)
      || str.key.startsWith("OTP", ignoreCase = true)
      || str.key.startsWith("HmacOtp", ignoreCase = true)
      || str.key.startsWith("TimeOtp", ignoreCase = true)
    ) {
      return true
    }
  }
  return false
}

fun PwEntryV4.isCollection(): Boolean {
  val value = strings[Constance.KPA_IS_COLLECTION]
  return value != null && value.toString().equals("true", true)
}

fun PwEntryV4.setCollection(isCollection: Boolean) {
  this.strings[Constance.KPA_IS_COLLECTION] = ProtectedString(false, isCollection.toString())
}

inline fun RecyclerView.doOnItemClickListener(
  crossinline action: (
    rv: RecyclerView,
    position: Int,
    v: View
  ) -> Unit
) {
  RvItemClickSupport.addTo(this)
    .setOnItemClickListener { rv, position, v ->
      return@setOnItemClickListener action.invoke(rv, position, v)
    }
}

inline fun RecyclerView.doOnItemLongClickListener(
  crossinline action: (
    rv: RecyclerView,
    position: Int,
    v: View
  ) -> Boolean
) {
  RvItemClickSupport.addTo(this)
    .setOnItemLongClickListener { rv, position, v ->
      return@setOnItemLongClickListener action.invoke(rv, position, v)
    }
}

inline fun RecyclerView.doOnTouchEvent(
  crossinline action: (
    rv: RecyclerView,
    e: MotionEvent
  ) -> Unit
) = addOnItemTouchListener(onTouchEvent = action)

inline fun RecyclerView.doOnInterceptTouchEvent(
  crossinline action: (rv: RecyclerView, e: MotionEvent) -> Boolean
) = addOnItemTouchListener(onInterceptTouchEvent = action)

inline fun RecyclerView.doOnRequestDisallowInterceptTouchEvent(
  crossinline action: (disallowIntercept: Boolean) -> Unit
) = addOnItemTouchListener(onRequestDisallowInterceptTouchEvent = action)

inline fun RecyclerView.addOnItemTouchListener(
  crossinline onTouchEvent: (
    rv: RecyclerView,
    e: MotionEvent
  ) -> Unit = { _, _ -> },
  crossinline onInterceptTouchEvent: (
    rv: RecyclerView,
    e: MotionEvent
  ) -> Boolean = { _, _ -> false },
  crossinline onRequestDisallowInterceptTouchEvent: (
    disallowIntercept: Boolean
  ) -> Unit = { _ -> }
): OnItemTouchListener {
  val touchListener = object : OnItemTouchListener {
    override fun onTouchEvent(
      rv: RecyclerView,
      e: MotionEvent
    ) {
      onTouchEvent.invoke(rv, e)
    }

    override fun onInterceptTouchEvent(
      rv: RecyclerView,
      e: MotionEvent
    ): Boolean {
      return onInterceptTouchEvent.invoke(rv, e)
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
      onRequestDisallowInterceptTouchEvent.invoke(disallowIntercept)
    }
  }

  addOnItemTouchListener(touchListener)
  return touchListener
}