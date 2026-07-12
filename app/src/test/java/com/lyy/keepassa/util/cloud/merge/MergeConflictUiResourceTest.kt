package com.lyy.keepassa.util.cloud.merge

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Node

class MergeConflictUiResourceTest {
  @Test
  fun finishShowsLoadingBeforeSubmittingResolvedResult() {
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/merge/MergeConflictActivity.kt").readText()
    val sendResolved = source.substringAfter("private fun sendResolved()").substringBefore("private fun sendCancel()")

    val showIndex = sendResolved.indexOf("showCompletionLoadingDialog()")
    val completeIndex = sendResolved.indexOf("session.tryComplete(")
    assertTrue(showIndex >= 0)
    assertTrue(completeIndex > showIndex)
    assertTrue(source.contains("class MergeConflictActivity : FragmentActivity()"))
    assertTrue(source.contains("completionLoadingDialog.showNow("))
    assertTrue(source.contains("supportFragmentManager"))
    assertTrue(source.contains("session.processingCompleted.await()"))
    assertTrue(source.contains("completionLoadingDialog.dismiss"))
  }


  private val androidNamespace = "http://schemas.android.com/apk/res/android"
  private val appNamespace = "http://schemas.android.com/apk/res-auto"

  @Test
  fun mergeConflictActivityUsesProjectToolbarAndRecyclerViewLayout() {
    val document = layout("activity_merge_conflict")

    assertEquals(
      "com.google.android.material.appbar.MaterialToolbar",
      document.nodeById("mergeToolbar").nodeName
    )
    assertEquals("52dp", document.nodeById("mergeToolbar").androidAttribute("layout_height"))
    assertEquals("@style/MergeConflict.Toolbar.TitleText", document.nodeById("mergeToolbar").appAttribute("titleTextAppearance"))
    assertEquals("@style/MergeConflict.Toolbar.SubtitleText", document.nodeById("mergeToolbar").appAttribute("subtitleTextAppearance"))
    assertEquals("false", document.nodeById("mergeToolbar").appAttribute("titleCentered"))
    assertEquals("androidx.recyclerview.widget.RecyclerView", document.nodeById("mergeConflictList").nodeName)
    assertEquals("40dp", document.nodeById("mergeBottomBar").androidAttribute("layout_height"))
    listOf("mergeCancel", "mergePrevious", "mergeNext").forEach { id ->
      val action = document.nodeById(id)
      assertEquals("64dp", action.androidAttribute("minWidth"))
      assertEquals("8dp", action.androidAttribute("paddingStart"))
      assertEquals("8dp", action.androidAttribute("paddingEnd"))
      assertEquals("@dimen/text_size_small", action.androidAttribute("textSize"))
    }
  }

  @Test
  fun mergeConflictActivityShowsConflictTitleAndEntrySubtitleInToolbar() {
    val document = layout("activity_merge_conflict")
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/merge/MergeConflictActivity.kt").readText()

    assertFalse(document.hasNodeById("mergeItemTitle"))

    val progress = document.nodeById("mergeProgress")
    assertEquals("@string/merge_conflict_progress", progress.androidAttribute("text"))
    assertEquals("@dimen/text_size_small", progress.androidAttribute("textSize"))
    assertEquals("0dp", progress.androidAttribute("layout_width"))

    assertTrue(source.contains("binding.mergeToolbar.setTitle(R.string.merge_conflict_title)"))
    assertTrue(source.contains("binding.mergeToolbar.subtitle = itemTitle(item.local)"))
    assertTrue(source.contains("binding.mergeToolbar.subtitle = null"))
    assertFalse(source.contains("binding.mergeItemTitle"))
    assertTrue(source.contains("binding.mergeProgress.text = getString("))
  }

  @Test
  fun mergeConflictFieldRowUsesCompactProjectTypography() {
    val document = layout("item_merge_conflict_field")

    assertEquals("@dimen/text_size_smallest", document.nodeById("fieldName").androidAttribute("textSize"))
    listOf("cloudValue", "resultValue", "localValue").forEach { id ->
      val node = document.nodeById(id)
      assertEquals("@dimen/text_size_smaller", node.androidAttribute("textSize"))
      assertEquals("3", node.androidAttribute("maxLines"))
      assertEquals("@drawable/bg_ripple_white_selector", node.androidAttribute("background"))
    }
  }

  @Test
  fun mergeConflictActivityDoesNotUsePlatformButtonsForFieldChoices() {
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/merge/MergeConflictActivity.kt").readText()

    assertFalse(source.contains("import android.widget.Button"))
    assertFalse(source.contains("Button(this)"))
  }

  @Test
  fun mergeConflictActivityUsesLandscapeAutoSizeDesignWidth() {
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/merge/MergeConflictActivity.kt").readText()

    assertTrue(source.contains("CustomAdapt"))
    assertTrue(source.contains("LANDSCAPE_DESIGN_WIDTH_DP = 731f"))
    assertTrue(source.contains("AutoSizeConfig.getInstance().screenWidth = maxOf"))
    assertTrue(source.contains("AutoSize.autoConvertDensity(this, LANDSCAPE_DESIGN_WIDTH_DP, true)"))
  }

  @Test
  fun mergeConflictActivityUsesImmersiveFullscreen() {
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/merge/MergeConflictActivity.kt").readText()

    assertTrue(source.contains("WindowCompat.setDecorFitsSystemWindows(window, false)"))
    assertTrue(source.contains("WindowInsetsControllerCompat(window, window.decorView)"))
    assertTrue(source.contains("hide(WindowInsetsCompat.Type.systemBars())"))
    assertTrue(source.contains("BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE"))
    assertTrue(source.contains("LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES"))
    assertTrue(source.contains("override fun onWindowFocusChanged(hasFocus: Boolean)"))
  }

  @Test
  fun mergeConflictActivityRendersBinaryFileNameInValueCells() {
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/merge/MergeConflictActivity.kt").readText()

    assertTrue(source.contains("binaryText(it)"))
    assertFalse(source.contains("binaryText(it.value)"))
    assertTrue(source.contains("merge_binary_named_summary"))
  }

  @Test
  fun mergeConflictActivityDoesNotResolveBinaryFileNameFromUiState() {
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/merge/MergeConflictActivity.kt").readText()

    assertFalse(source.contains("BinaryAttachmentNameResolver.resolve"))
    assertTrue(source.contains("fieldValueText(conflict.cloud, conflict.renderer)"))
    assertTrue(source.contains("fieldValueText(conflict.local, conflict.renderer)"))
  }

  @Test
  fun mergeConflictStringsHaveChineseLocalizations() {
    val simplified = strings("src/main/res/values-zh-rCN/strings.xml")
    val traditional = strings("src/main/res/values-zh-rTW/strings.xml")

    assertEquals("解决合并冲突", simplified["merge_conflict_title"])
    assertEquals("%1\$d/%2\$d", simplified["merge_conflict_progress"])
    assertEquals("合并会话已过期，请重新同步。", simplified["merge_conflict_session_expired"])
    assertEquals("云端", simplified["merge_field_cloud"])
    assertEquals("结果", simplified["merge_field_result"])
    assertEquals("本地", simplified["merge_field_local"])
    assertEquals("上一条", simplified["merge_previous"])
    assertEquals("下一条", simplified["merge_next"])
    assertEquals("完成", simplified["merge_finish"])
    assertEquals("未选择", simplified["merge_not_selected"])
    assertEquals("请为每个冲突字段选择本地或云端版本。", simplified["merge_decision_required"])
    assertEquals("仅本地存在的条目", simplified["merge_local_only_title"])
    assertEquals("勾选的条目会从本地删除；未勾选的条目会保留。", simplified["merge_local_only_desc"])
    assertEquals("群组名称", simplified["merge_field_group_name"])
    assertEquals("群组备注", simplified["merge_field_group_notes"])
    assertEquals("已展开", simplified["merge_field_group_is_expanded"])
    assertEquals("默认自动输入序列", simplified["merge_field_group_default_auto_type_sequence"])
    assertEquals("启用自动输入", simplified["merge_field_group_enable_auto_type"])
    assertEquals("启用搜索", simplified["merge_field_group_enable_searching"])
    assertEquals("最后顶部可见条目", simplified["merge_field_group_last_top_visible_entry"])
    assertEquals("图标", simplified["merge_field_icon"])
    assertEquals("自定义图标", simplified["merge_field_custom_icon"])
    assertEquals("标签", simplified["merge_field_tags"])
    assertEquals("过期时间", simplified["merge_field_expire_date"])
    assertEquals("启用过期", simplified["merge_field_expires"])
    assertEquals("UUID", simplified["merge_field_uuid"])
    assertEquals("前景色", simplified["merge_field_foreground_color"])
    assertEquals("背景色", simplified["merge_field_background_color"])
    assertEquals("覆盖 URL", simplified["merge_field_override_url"])
    assertEquals("自动输入", simplified["merge_field_auto_type"])
    assertEquals("位置变更时间", simplified["merge_field_location_changed"])
    assertEquals("创建时间", simplified["merge_field_creation_time"])
    assertEquals("最后修改时间", simplified["merge_field_last_modification_time"])
    assertEquals("最后访问时间", simplified["merge_field_last_access_time"])
    assertEquals("使用次数", simplified["merge_field_usage_count"])
    assertEquals("内部 URL", simplified["merge_field_internal_url"])
    assertEquals("附加数据", simplified["merge_field_additional"])
    assertEquals("自定义数据", simplified["merge_field_custom_data"])
    assertEquals("上一个父群组", simplified["merge_field_previous_parent_group"])
    assertEquals("质量检查", simplified["merge_field_quality_check"])
    assertEquals("附件，%1\$d 字节", simplified["merge_binary_summary"])
    assertEquals("%1\$s\n附件，%2\$d 字节", simplified["merge_binary_named_summary"])
    assertEquals("图标 #%1\$d", simplified["merge_icon_summary"])
    assertEquals("本地合并已保存，但上传失败。下次同步会重试。", simplified["merge_upload_failed_retry_next_time"])
    assertEquals("整条数据", simplified["merge_field_whole_item"])
    assertTrue(simplified["merge_legacy_entry_summary"].orEmpty().contains("标题：%1\$s"))
    assertTrue(simplified["merge_legacy_group_summary"].orEmpty().contains("名称：%1\$s"))

    assertEquals("解決合併衝突", traditional["merge_conflict_title"])
    assertEquals("%1\$d/%2\$d", traditional["merge_conflict_progress"])
    assertEquals("合併會話已過期，請重新同步。", traditional["merge_conflict_session_expired"])
    assertEquals("雲端", traditional["merge_field_cloud"])
    assertEquals("結果", traditional["merge_field_result"])
    assertEquals("本機", traditional["merge_field_local"])
    assertEquals("上一條", traditional["merge_previous"])
    assertEquals("下一條", traditional["merge_next"])
    assertEquals("完成", traditional["merge_finish"])
    assertEquals("未選擇", traditional["merge_not_selected"])
    assertEquals("請為每個衝突欄位選擇本機或雲端版本。", traditional["merge_decision_required"])
    assertEquals("僅本機存在的條目", traditional["merge_local_only_title"])
    assertEquals("勾選的條目會從本機刪除；未勾選的條目會保留。", traditional["merge_local_only_desc"])
    assertEquals("群組名稱", traditional["merge_field_group_name"])
    assertEquals("群組備註", traditional["merge_field_group_notes"])
    assertEquals("已展開", traditional["merge_field_group_is_expanded"])
    assertEquals("預設自動輸入序列", traditional["merge_field_group_default_auto_type_sequence"])
    assertEquals("啟用自動輸入", traditional["merge_field_group_enable_auto_type"])
    assertEquals("啟用搜尋", traditional["merge_field_group_enable_searching"])
    assertEquals("最後頂部可見條目", traditional["merge_field_group_last_top_visible_entry"])
    assertEquals("圖示", traditional["merge_field_icon"])
    assertEquals("自訂圖示", traditional["merge_field_custom_icon"])
    assertEquals("標籤", traditional["merge_field_tags"])
    assertEquals("過期時間", traditional["merge_field_expire_date"])
    assertEquals("啟用過期", traditional["merge_field_expires"])
    assertEquals("UUID", traditional["merge_field_uuid"])
    assertEquals("前景色", traditional["merge_field_foreground_color"])
    assertEquals("背景色", traditional["merge_field_background_color"])
    assertEquals("覆寫 URL", traditional["merge_field_override_url"])
    assertEquals("自動輸入", traditional["merge_field_auto_type"])
    assertEquals("位置變更時間", traditional["merge_field_location_changed"])
    assertEquals("建立時間", traditional["merge_field_creation_time"])
    assertEquals("最後修改時間", traditional["merge_field_last_modification_time"])
    assertEquals("最後存取時間", traditional["merge_field_last_access_time"])
    assertEquals("使用次數", traditional["merge_field_usage_count"])
    assertEquals("內部 URL", traditional["merge_field_internal_url"])
    assertEquals("附加資料", traditional["merge_field_additional"])
    assertEquals("自訂資料", traditional["merge_field_custom_data"])
    assertEquals("上一個父群組", traditional["merge_field_previous_parent_group"])
    assertEquals("品質檢查", traditional["merge_field_quality_check"])
    assertEquals("附件，%1\$d 位元組", traditional["merge_binary_summary"])
    assertEquals("%1\$s\n附件，%2\$d 位元組", traditional["merge_binary_named_summary"])
    assertEquals("圖示 #%1\$d", traditional["merge_icon_summary"])
    assertEquals("本機合併已儲存，但上傳失敗。下次同步會重試。", traditional["merge_upload_failed_retry_next_time"])
    assertEquals("整筆資料", traditional["merge_field_whole_item"])
    assertTrue(traditional["merge_legacy_entry_summary"].orEmpty().contains("標題：%1\$s"))
    assertTrue(traditional["merge_legacy_group_summary"].orEmpty().contains("名稱：%1\$s"))
  }

  private fun layout(name: String): Document {
    return DocumentBuilderFactory.newInstance().apply {
      isNamespaceAware = true
    }.newDocumentBuilder()
      .parse(File("src/main/res/layout/$name.xml"))
  }

  private fun strings(path: String): Map<String, String> {
    val document = DocumentBuilderFactory.newInstance().apply {
      isNamespaceAware = true
    }.newDocumentBuilder()
      .parse(File(path))
    val nodes = document.getElementsByTagName("string")
    return buildMap {
      for (i in 0 until nodes.length) {
        val node = nodes.item(i)
        val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
        put(name, node.textContent)
      }
    }
  }

  private fun Document.nodeById(id: String): Node {
    val expected = "@+id/$id"
    val nodes = getElementsByTagName("*")
    for (i in 0 until nodes.length) {
      val node = nodes.item(i)
      if (node.androidAttribute("id") == expected) {
        return node
      }
    }
    throw AssertionError("Missing view id: $expected")
  }

  private fun Document.hasNodeById(id: String): Boolean {
    val expected = "@+id/$id"
    val nodes = getElementsByTagName("*")
    for (i in 0 until nodes.length) {
      if (nodes.item(i).androidAttribute("id") == expected) {
        return true
      }
    }
    return false
  }

  private fun Node.androidAttribute(name: String): String? {
    return attributes?.getNamedItemNS(androidNamespace, name)?.nodeValue
  }

  private fun Node.appAttribute(name: String): String? {
    return attributes?.getNamedItemNS(appNamespace, name)?.nodeValue
  }
}
