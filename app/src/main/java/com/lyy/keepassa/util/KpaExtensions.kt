/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.text.TextUtils
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import com.arialyy.frame.util.ReflectionUtil
import com.arialyy.frame.util.ResUtil
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.BarUtils
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.Constance
import com.lyy.keepassa.entity.HmacOtpBean
import com.lyy.keepassa.entity.KeepassBean
import com.lyy.keepassa.entity.KeepassXcBean
import com.lyy.keepassa.entity.TimeOtp2Bean
import com.lyy.keepassa.entity.TrayTotpBean
import com.lyy.keepassa.util.totp.ComposeKeeTrayTotp
import com.lyy.keepassa.util.totp.ComposeKeepass
import com.lyy.keepassa.util.totp.ComposeKeepass.HmacOtp_Secret
import com.lyy.keepassa.util.totp.ComposeKeepass.TimeOtp_Secret
import com.lyy.keepassa.util.totp.ComposeKeepassxc
import com.lyy.keepassa.util.totp.ComposeKeepassxc.KEY_STEAM
import com.lyy.keepassa.util.totp.OtpUtil
import com.lyy.keepassa.util.totp.SecretHexType
import com.lyy.keepassa.util.totp.TokenCalculator
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm
import timber.log.Timber
import kotlin.math.abs

val charRegex = Regex("[^a-zA-Z0-9]")

fun View.handleBottomEdge(callback: (View, Int) -> Unit) {
  ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->

    if (!BarUtils.isNavBarVisible(ActivityUtils.getTopActivity())) {
      return@setOnApplyWindowInsetsListener insets
    }

    if (isGestureBarVisible(this)){
      callback.invoke(this, getGestureBarHeight(insets))
      return@setOnApplyWindowInsetsListener insets
    }

    if (insets.isVisible(WindowInsetsCompat.Type.navigationBars())) {
      callback.invoke(this, abs(BarUtils.getNavBarHeight()))
      return@setOnApplyWindowInsetsListener WindowInsetsCompat.CONSUMED
    }

    insets
  }
}

fun getGestureBarHeight(insets: WindowInsetsCompat): Int {
  return insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
}

fun isGestureBarVisible(view: View): Boolean {
  val insets = ViewCompat.getRootWindowInsets(view) ?: return false

  // 获取系统手势区域和导航栏区域
  val gestureInsets = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
  val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

  // 手势栏可见的条件：
  // 1. 系统手势区域大于导航栏区域（手势模式下）
  // 2. 导航栏本身可见（非全屏模式）
  return (gestureInsets.bottom > navBarInsets.bottom) &&
    (navBarInsets.bottom > 0)
}

fun ViewGroup.handleBottomEdge(callback: (View, Int) -> Unit) {
  ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->

    if (!BarUtils.isNavBarVisible(ActivityUtils.getTopActivity())) {
      return@setOnApplyWindowInsetsListener insets
    }

    if (isGestureBarVisible(this)){
      callback.invoke(this, getGestureBarHeight(insets))
      return@setOnApplyWindowInsetsListener insets
    }

    if (insets.isVisible(WindowInsetsCompat.Type.navigationBars())) {
      callback.invoke(this, abs(BarUtils.getNavBarHeight()))
      return@setOnApplyWindowInsetsListener WindowInsetsCompat.CONSUMED
    }

    insets
  }
}

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
 * 设置中心锚点
 */
fun View.pivotCenter() {
  pivotX = measuredWidth / 2f
  pivotY = measuredHeight / 2f
}

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

@SuppressLint("RestrictedApi")
fun PopupMenu.init(menuId: Int, onItemCLick: (MenuItem) -> Unit): PopupMenu {
  val inflater: MenuInflater = menuInflater
  inflater.inflate(menuId, this.menu)
  // 以下代码为强制显示icon
  val mPopup = ReflectionUtil.getField(PopupMenu::class.java, "mPopup")
  mPopup.isAccessible = true
  val help = mPopup.get(this) as MenuPopupHelper
  help.setForceShowIcon(true)
  setOnMenuItemClickListener {
    onItemCLick.invoke(it)
    return@setOnMenuItemClickListener true
  }
  return this
}

fun Activity.isDestroy() = isDestroyed || isFinishing

/**
 * @return true has special char
 */
fun CharSequence.hasSpecialChar(): Boolean {
  return charRegex.containsMatchIn(this)
}

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
    if (str.key.equals(PwEntryV4.STR_NOTES, true) && !TextUtils.isEmpty(str.value.toString())) {
      return true
    }
  }
  return false
}

fun Map<String, ProtectedString>.hasTOTP(): Boolean {
  for (str in this) {
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
      || str.key.startsWith(ComposeKeepass.HmacOtp, ignoreCase = true)
      || str.key.startsWith(ComposeKeepass.TimeOtp, ignoreCase = true)
    ) {
      return true
    }
  }
  return false
}

fun PwEntryV4.hasTOTP(): Boolean {
  return strings.hasTOTP()
}

fun PwEntryV4.isCollectioned(): Boolean {
  val value = strings[Constance.KPA_IS_COLLECTION]
  return value != null && value.toString().equals("true", true)
}

fun PwEntryV4.setCollection(isCollection: Boolean) {
  this.strings[Constance.KPA_IS_COLLECTION] = ProtectedString(false, isCollection.toString())
}

fun PwEntry.copyUserName() {
  val userName = KdbUtil.getUserName(this)
  ClipboardUtil.get()
    .copyDataToClip(userName)
  HitUtil.toaskShort(ResUtil.getString(R.string.hint_copy_user))
}

fun PwEntry.copyPassword() {
  val pass = KdbUtil.getPassword(this)
  ClipboardUtil.get()
    .copyDataToClip(pass)
  HitUtil.toaskShort(ResUtil.getString(R.string.hint_copy_pass))
}

fun PwEntryV4.copyTotp() {
  val pass = OtpUtil.getOtpPass(this).second
  if (pass.isNullOrBlank()) {
    HitUtil.toaskShort(ResUtil.getString(R.string.totp_key_error))
    return
  }
  ClipboardUtil.get()
    .copyDataToClip(pass)
  HitUtil.toaskShort(ResUtil.getString(R.string.hint_copy_totp))
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

fun PwEntryV4.removeAttrFile(key: String) {
  binaries.remove(key)
}

fun PwEntryV4.removeAttrStr(str: String) {
  strings.remove(str)
}

fun PwEntryV4.otpIsKeeTrayTotp(): Boolean {
  return strings[ComposeKeeTrayTotp.KEY_SETTING] != null
}

fun PwEntryV4.otpIsKeeTraySteam(): Boolean {
  val otpSettings = strings[ComposeKeeTrayTotp.KEY_SETTING]
  if (otpSettings != null) {
    val tempArray = otpSettings.toString()
      .split(";")
    return tempArray[1] == "S"
  }

  return false
}

fun PwEntryV4.getKeeTrayBean(): TrayTotpBean {
  if (!otpIsKeeTrayTotp()) {
    throw IllegalAccessException("not kray otp")
  }
  val totpSetting = strings[ComposeKeeTrayTotp.KEY_SETTING]
  val array = totpSetting.toString()
    .split(";")
  return TrayTotpBean(
    secret = strings[ComposeKeeTrayTotp.KEY_SEED].toString(),
    period = array[0].toInt(),
    isSteam = array[1] == "s"
  )
}

fun PwEntryV4.otpIsKeepassXcSteam(): Boolean {
  val seed = strings[ComposeKeepassxc.KEY_SEED]?.toString()
  val uri = Uri.parse(seed)
  val encoder = uri.getQueryParameter(ComposeKeepassxc.KEY_ENCODER)

  if (encoder != null && encoder.equals(KEY_STEAM, ignoreCase = true)) {
    return true
  }
  return false
}

fun PwEntryV4.otpKeepassXC(): Boolean {
  return strings[ComposeKeepassxc.KEY_SEED]?.toString()
    ?.startsWith("otpauth", ignoreCase = true) == true
}

fun PwEntryV4.getKeepassXcBean(): KeepassXcBean {
  if (!otpKeepassXC()) {
    throw IllegalAccessException("not keepassxc otp")
  }
  val seed = strings[ComposeKeepassxc.KEY_SEED]?.toString()
  val uri = Uri.parse(seed)
  val algorithm = uri.getQueryParameter(ComposeKeepassxc.KEY_ALGORITHM)

  return KeepassXcBean(
    host = uri.host ?: "totp",
    title = getRealTitle(),
    userName = getRealUserName(),
    isSteam = otpIsKeepassXcSteam(),
    encoder = uri.getQueryParameter(ComposeKeepassxc.KEY_ENCODER) ?: "",
    secret = uri.getQueryParameter(ComposeKeepassxc.KEY_SECRET) ?: "",
    issuer = uri.getQueryParameter(ComposeKeepassxc.KEY_ISSUER) ?: "",
    period = uri.getQueryParameter(ComposeKeepassxc.KEY_PERIOD)?.toInt()
      ?: TokenCalculator.TOTP_DEFAULT_PERIOD,
    digits = uri.getQueryParameter(ComposeKeepassxc.KEY_DIGITS)?.toInt()
      ?: TokenCalculator.TOTP_DEFAULT_DIGITS,
    algorithm = when (algorithm) {
      "SHA256" -> HashAlgorithm.SHA256
      "SHA512" -> HashAlgorithm.SHA512
      else -> HashAlgorithm.SHA1
    },
    counter = uri.getQueryParameter(ComposeKeepassxc.KEY_COUNTER) ?: "",
  )
}

fun PwEntryV4.otpKeepass(): Boolean {
  for (str in strings) {
    val key = str.toString()
    if (key.startsWith(TimeOtp_Secret) || key.startsWith(HmacOtp_Secret)) {
      return true
    }
  }
  return false
}

fun PwEntryV4.getKeepassBean(): KeepassBean {
  var otpBean: TimeOtp2Bean? = null
  var hmacBean: HmacOtpBean? = null

  fun isHmacOtp(): Boolean {
    strings.forEach {
      if (it.toString().startsWith(ComposeKeepass.HmacOtp)) {
        return true
      }
    }
    return false
  }

  fun isTotp(): Boolean {
    strings.forEach {
      if (it.toString().startsWith(ComposeKeepass.TimeOtp)) {
        return true
      }
    }
    return false
  }

  if (isHmacOtp()) {
    val secretType: SecretHexType
    val secret = when {
      strings[ComposeKeepass.HmacOtp_Secret_Base32] != null -> {
        secretType = SecretHexType.BASE_32
        strings[ComposeKeepass.HmacOtp_Secret_Base32].toString()
      }

      strings[ComposeKeepass.HmacOtp_Secret_Base64] != null -> {
        secretType = SecretHexType.BASE_64
        strings[ComposeKeepass.HmacOtp_Secret_Base64].toString()
      }

      strings[ComposeKeepass.HmacOtp_Secret_Hex] != null -> {
        secretType = SecretHexType.HEX
        strings[ComposeKeepass.HmacOtp_Secret_Hex].toString()
      }

      else -> {
        secretType = SecretHexType.UTF_8
        strings[HmacOtp_Secret].toString()
      }
    }

    hmacBean = HmacOtpBean(
      secretType = secretType,
      secret = secret,
      algorithm = HashAlgorithm.SHA1,
      counter = strings[ComposeKeepass.HmacOtp_Counter]?.toString()?.toInt()
        ?: TokenCalculator.HOTP_INITIAL_COUNTER,
      len = TokenCalculator.TOTP_DEFAULT_DIGITS
    )
  }

  if (isTotp()) {
    val secretType: SecretHexType
    val secret = when {
      strings[ComposeKeepass.TimeOtp_Secret_Base32] != null -> {
        secretType = SecretHexType.BASE_32
        strings[ComposeKeepass.TimeOtp_Secret_Base32].toString()
      }

      strings[ComposeKeepass.TimeOtp_Secret_Base64] != null -> {
        secretType = SecretHexType.BASE_64
        strings[ComposeKeepass.TimeOtp_Secret_Base64].toString()
      }

      strings[ComposeKeepass.TimeOtp_Secret_Hex] != null -> {
        secretType = SecretHexType.HEX
        strings[ComposeKeepass.TimeOtp_Secret_Hex].toString()
      }

      else -> {
        secretType = SecretHexType.UTF_8
        strings[TimeOtp_Secret].toString()
      }
    }

    val algorithm = when (strings[ComposeKeepass.TimeOtp_Algorithm].toString()) {
      ComposeKeepass.HMAC_SHA_256 -> HashAlgorithm.SHA256
      ComposeKeepass.HMAC_SHA_512 -> HashAlgorithm.SHA512
      else -> HashAlgorithm.SHA1
    }

    otpBean = TimeOtp2Bean(
      secretType = secretType,
      secret = secret,
      digits = strings[ComposeKeepass.TimeOtp_Length]?.toString()?.toInt()
        ?: TokenCalculator.TOTP_DEFAULT_DIGITS,
      algorithm = algorithm,
      period = strings[ComposeKeepass.TimeOtp_Period]?.toString()?.toInt()
        ?: TokenCalculator.TOTP_DEFAULT_PERIOD
    )
  }

  return KeepassBean(
    otpBean,
    hmacBean
  )
}

fun PwEntryV4.otpIsKeepOtp(): Boolean {
  val seed = strings["otp"]?.toString()
  if (seed?.startsWith("key") == true) {
    return true
  }
  return false
}

/**
 * 判断是否是KeeOtp2插件
 */
fun PwEntryV4.otpIsKeeOtp2(): Boolean {
  for (str in strings) {
    val key = str.toString()
    if (key.startsWith("TimeOtp") || key.startsWith("HmacOtp")) {
      return true
    }
  }
  return false
}

fun PwEntry.getRealTitle(): String {
  if (BaseApp.KDB?.pm == null) {
    return ""
  }
  return if (isRef()) getTitle(true, BaseApp.KDB!!.pm) else title
}

fun PwEntry.getRealUserName(): String {
  if (BaseApp.KDB?.pm == null) {
    return ""
  }

  return if (isRef())
    getUsername(true, BaseApp.KDB!!.pm)
  else
    username
}

fun PwEntry.getRealPass(): String {
  if (BaseApp.KDB?.pm == null) {
    return ""
  }

  return if (isRef())
    getPassword(true, BaseApp.KDB!!.pm)
  else
    password
}