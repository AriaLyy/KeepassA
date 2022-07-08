package com.lyy.keepassa.util

import android.os.Parcelable

/**
 * K>V持久化
 */
interface KVStorage {

    fun put(key: String, value: String): Boolean

    fun getString(key: String, defaultValue: String = ""): String

    fun put(key: String, value: Boolean): Boolean

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean

    fun put(key: String, value: Int): Boolean

    fun getInt(key: String, defaultValue: Int = 0): Int

    fun put(key: String, value: Long): Boolean

    fun getLong(key: String, defaultValue: Long = 0L): Long

    fun put(key: String, value: Float): Boolean

    fun getFloat(key: String, defaultValue: Float = 0F): Float

    fun put(key: String, value: Double): Boolean

    fun getDouble(key: String, defaultValue: Double = 0.0): Double

    fun put(key: String, value: Set<String>): Boolean

    fun getStringSet(key: String, defaultValue: Set<String> = setOf()): Set<String>

    fun put(key: String, value: Parcelable): Boolean

    fun <T : Parcelable?> get(key: String?, tClass: Class<T>?, defaultValue: T?): T?

    fun containsKey(key: String): Boolean

    fun remove(key: String)

    fun clean()
}