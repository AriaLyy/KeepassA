package com.lyy.keepassa.event

import com.keepassdroid.database.PwEntry

/**
 * 创建条目事件
 */
data class CreateOrUpdateEntryEvent(val entry: PwEntry, val isUpdate: Boolean)