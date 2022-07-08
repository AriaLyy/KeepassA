package com.lyy.keepassa.util

import android.os.Parcelable
import com.tencent.mmkv.MMKV

object CommonKVStorage : KVStorage {

  private val kv: MMKV by lazy {
    MMKV.mmkvWithID("com.lyy.kpa", MMKV.MULTI_PROCESS_MODE)
  }

  override fun put(key: String, value: String): Boolean = kv.encode(key, value)

  override fun getString(key: String, defaultValue: String): String =
    kv.decodeString(key, defaultValue) ?: ""

  override fun put(key: String, value: Boolean): Boolean = kv.encode(key, value)

  override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
    kv.decodeBool(key, defaultValue)

  override fun put(key: String, value: Int): Boolean = kv.encode(key, value)

  override fun getInt(key: String, defaultValue: Int): Int = kv.decodeInt(key, defaultValue)

  override fun put(key: String, value: Long): Boolean = kv.encode(key, value)

  override fun getLong(key: String, defaultValue: Long): Long = kv.decodeLong(key, defaultValue)

  override fun put(key: String, value: Float): Boolean = kv.encode(key, value)

  override fun getFloat(key: String, defaultValue: Float): Float =
    kv.decodeFloat(key, defaultValue)

  override fun put(key: String, value: Double): Boolean = kv.encode(key, value)

  override fun getDouble(key: String, defaultValue: Double): Double =
    kv.decodeDouble(key, defaultValue)

  override fun put(key: String, value: Set<String>): Boolean = kv.encode(key, value)

  override fun getStringSet(key: String, defaultValue: Set<String>): Set<String> =
    kv.decodeStringSet(key, defaultValue) ?: mutableSetOf()

  override fun put(key: String, value: Parcelable): Boolean = kv.encode(key, value)

  override fun <T : Parcelable?> get(key: String?, tClass: Class<T>?, defaultValue: T?): T? =
    kv.decodeParcelable(key, tClass, defaultValue)

  override fun containsKey(key: String): Boolean = kv.containsKey(key)

  override fun remove(key: String) = kv.removeValueForKey(key)

  override fun clean() = kv.clearAll()
}