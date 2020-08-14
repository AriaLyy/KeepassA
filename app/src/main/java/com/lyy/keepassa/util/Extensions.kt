package com.lyy.keepassa.util

import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.DbRecord
import com.lyy.keepassa.view.DbPathType
import com.lyy.keepassa.view.DbPathType.AFS
import java.io.Serializable

fun <T> Fragment.putArgument(
  key: String,
  value: T
) {
  if (this.arguments == null) {
    this.arguments = Bundle()
  }

  when (value) {
    is Int -> this.requireArguments()
        .putInt(key, value)
    is Boolean -> this.requireArguments()
        .putBoolean(key, value)
    is String -> this.requireArguments()
        .putString(key, value)
    is CharSequence -> this.requireArguments()
        .putCharSequence(key, value)
    is Float -> this.requireArguments()
        .putFloat(key, value)
    is Long -> this.requireArguments()
        .putLong(key, value)
    is Bundle -> this.requireArguments()
        .putBundle(key, value)
    is Serializable -> this.requireArguments()
        .putSerializable(key, value)
    is Parcelable -> this.requireArguments()
        .putParcelable(key, value)
    is Char -> this.requireArguments()
        .putChar(key, value)
    is Byte -> this.requireArguments()
        .putByte(key, value)
    else -> error("不支持的类型, $value")
  }
}

fun <T> Fragment.getArgument(key: String): T? {
  if (this.arguments == null) {
    return null
  }
  val d = this.arguments?.get(key) ?: return null
  return d as T
}

fun DbRecord.isAFS(): Boolean {
  return DbPathType.valueOf(this.type) === AFS
}

//fun <U, T> Fragment.getArgument(key: String) = BindLoader<U, T>(key)

//private class IntentDelegate<in U, out T>(private val key: String) : ReadOnlyProperty<U, T> {
//  override fun getValue(
//    thisRef: U,
//    property: KProperty<*>
//  ): T {
//    @Suppress("UNCHECKED_CAST")
//    return when (thisRef) {
//      is Fragment -> thisRef.arguments?.get(key) as T
//      else -> (thisRef as Activity).intent?.extras?.get(key) as T
//    }
//  }
//}
//
//class BindLoader<in U, out T>(private val key: String) {
//  operator fun provideDelegate(
//    thisRef: U,
//    prop: KProperty<*>
//  ): ReadOnlyProperty<U, T> {
//    // 创建委托
//    return IntentDelegate(key)
//  }
//
//}