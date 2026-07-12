package com.lyy.keepassa.util.cloud.merge

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.facade.annotation.Route
import com.blankj.utilcode.util.ScreenUtils
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.view.dialog.LoadingDialog
import com.lyy.keepassa.databinding.ActivityMergeConflictBinding
import com.lyy.keepassa.databinding.ItemMergeConflictFieldBinding
import com.lyy.keepassa.databinding.ItemMergeLocalOnlyBinding
import me.jessyan.autosize.AutoSize
import me.jessyan.autosize.AutoSizeConfig
import me.jessyan.autosize.internal.CustomAdapt
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

@Route(path = MergeConflictActivity.ROUTE)
class MergeConflictActivity : FragmentActivity(), CustomAdapt {

  private val model by lazy {
    ViewModelProvider(this)[MergeConflictViewModel::class.java]
  }
  private lateinit var binding: ActivityMergeConflictBinding
  private var showingLocalOnly = false
  private val completionLoadingDialog = LoadingDialog()

  override fun onCreate(savedInstanceState: Bundle?) {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    configureLandscapeAutoSize()
    super.onCreate(savedInstanceState)
    configureImmersiveFullscreen()
    binding = ActivityMergeConflictBinding.inflate(layoutInflater)
    setContentView(binding.root)

    val sessionId = intent.getStringExtra(KEY_SESSION_ID)
    val loadedSession = sessionId?.let { MergeConflictSessionStore.get(it) }
    if (loadedSession == null) {
      Toast.makeText(this, R.string.merge_conflict_session_expired, Toast.LENGTH_SHORT).show()
      finish()
      return
    }
    model.bind(loadedSession)
    binding.mergeToolbar.setNavigationOnClickListener { sendCancel() }
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        sendCancel()
      }
    })
    binding.mergeConflictList.layoutManager = LinearLayoutManager(this)
    bindBottomActions()
    render()
  }

  override fun isBaseOnWidth(): Boolean = true

  override fun getSizeInDp(): Float = LANDSCAPE_DESIGN_WIDTH_DP

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) {
      hideSystemBars()
    }
  }

  override fun onDestroy() {
    if (!model.completed && model.isBound()) {
      model.completed = true
      model.session.tryComplete(MergeConflictResult.Cancelled)
      MergeConflictSessionStore.remove(model.session.id)
    }
    super.onDestroy()
  }

  private fun configureLandscapeAutoSize() {
    val width = ScreenUtils.getScreenWidth()
    val height = ScreenUtils.getScreenHeight()
    AutoSizeConfig.getInstance().screenWidth = maxOf(width, height)
    AutoSizeConfig.getInstance().screenHeight = minOf(width, height)
    AutoSize.autoConvertDensity(this, LANDSCAPE_DESIGN_WIDTH_DP, true)
  }

  @Suppress("DEPRECATION")
  private fun configureImmersiveFullscreen() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = Color.TRANSPARENT
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      window.attributes = window.attributes.apply {
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
      }
    }
    hideSystemBars()
  }

  private fun hideSystemBars() {
    WindowInsetsControllerCompat(window, window.decorView).apply {
      systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      hide(WindowInsetsCompat.Type.systemBars())
    }
  }

  private fun bindBottomActions() {
    binding.mergeCancel.setOnClickListener { sendCancel() }
    binding.mergePrevious.setOnClickListener {
      if (showingLocalOnly) {
        showingLocalOnly = false
        model.itemIndex = model.session.items.lastIndex.coerceAtLeast(0)
      } else if (model.itemIndex > 0) {
        model.itemIndex--
      }
      render()
    }
    binding.mergeNext.setOnClickListener {
      if (showingLocalOnly || model.session.items.isEmpty()) {
        sendResolved()
        return@setOnClickListener
      }
      if (!currentItemResolved()) {
        Toast.makeText(this, R.string.merge_decision_required, Toast.LENGTH_SHORT).show()
        return@setOnClickListener
      }
      if (model.itemIndex < model.session.items.lastIndex) {
        model.itemIndex++
        render()
      } else if (model.session.localOnlyItems.isNotEmpty()) {
        renderLocalOnlyScreen()
      } else {
        sendResolved()
      }
    }
  }

  private fun render() {
    if (model.session.items.isNotEmpty() && !showingLocalOnly) {
      renderConflictItem()
    } else {
      renderLocalOnlyScreen()
    }
  }

  private fun renderConflictItem() {
    showingLocalOnly = false
    val item = model.session.items[model.itemIndex]
    val conflicts = item.autoMerge.conflicts
    binding.mergeToolbar.setTitle(R.string.merge_conflict_title)
    binding.mergeToolbar.subtitle = itemTitle(item.local)
    binding.mergeProgress.isVisible = true
    binding.mergeProgress.text = getString(
      R.string.merge_conflict_progress,
      model.itemIndex + 1,
      model.session.items.size
    )
    binding.mergeColumnHeader.isVisible = true
    binding.mergeLocalOnlyDesc.isVisible = false
    binding.mergePrevious.isVisible = true
    setActionEnabled(binding.mergePrevious, model.itemIndex > 0)
    binding.mergeNext.text = if (model.itemIndex == model.session.items.lastIndex &&
      model.session.localOnlyItems.isEmpty()
    ) {
      getString(R.string.merge_finish)
    } else {
      getString(R.string.merge_next)
    }
    binding.mergeConflictList.adapter = ConflictFieldAdapter(model.itemIndex, conflicts)
  }

  private fun renderLocalOnlyScreen() {
    showingLocalOnly = true
    binding.mergeToolbar.setTitle(R.string.merge_local_only_title)
    binding.mergeToolbar.subtitle = null
    binding.mergeColumnHeader.isVisible = false
    binding.mergeLocalOnlyDesc.isVisible = true
    binding.mergeProgress.isVisible = false
    binding.mergePrevious.isVisible = model.session.items.isNotEmpty()
    setActionEnabled(binding.mergePrevious, model.session.items.isNotEmpty())
    binding.mergeNext.text = getString(R.string.merge_finish)
    binding.mergeConflictList.adapter = LocalOnlyAdapter()
  }

  private fun currentItemResolved(): Boolean {
    val conflicts = model.session.items[model.itemIndex].autoMerge.conflicts
    val selected = model.decisions[model.itemIndex].orEmpty()
    return conflicts.all { selected.containsKey(it.key) }
  }

  private fun setActionEnabled(view: View, enabled: Boolean) {
    view.isEnabled = enabled
    view.alpha = if (enabled) 1f else 0.38f
  }

  private fun sendResolved() {
    if (model.completed) return
    showCompletionLoadingDialog()
    model.completed = true
    model.session.tryComplete(
      MergeConflictResult.Resolved(
        decisions = model.decisions,
        deleteLocalOnlyIndexes = model.deleteLocalOnlyIndexes
      )
    )
    lifecycleScope.launch {
      model.session.processingCompleted.await()
      if (completionLoadingDialog.isAdded) {
        completionLoadingDialog.dismissAllowingStateLoss()
      }
      MergeConflictSessionStore.remove(model.session.id)
      finish()
    }
  }

  private fun showCompletionLoadingDialog() {
    if (completionLoadingDialog.isAdded) return
    completionLoadingDialog.showNow(
      supportFragmentManager,
      COMPLETION_LOADING_DIALOG_TAG
    )
  }

  private fun sendCancel() {
    if (model.completed) {
      finish()
      return
    }
    model.completed = true
    model.session.tryComplete(MergeConflictResult.Cancelled)
    MergeConflictSessionStore.remove(model.session.id)
    finish()
  }

  private fun itemTitle(item: Any): String {
    return when (item) {
      is PwEntry -> item.title.orEmpty()
      is PwGroup -> item.name.orEmpty()
      else -> item.toString()
    }
  }

  private fun fieldLabel(key: FieldKey): String {
    return when (key) {
      is FieldKey.StringField -> key.key
      is FieldKey.BinaryField -> key.key
      is FieldKey.EntryPropertyField -> entryPropertyLabel(key.property)
      is FieldKey.GroupPropertyField -> groupPropertyLabel(key.property)
      FieldKey.GroupName -> getString(R.string.merge_field_group_name)
      FieldKey.GroupNotes -> getString(R.string.merge_field_group_notes)
      FieldKey.Icon -> getString(R.string.merge_field_icon)
      FieldKey.CustomIcon -> getString(R.string.merge_field_custom_icon)
      FieldKey.Tags -> getString(R.string.merge_field_tags)
      FieldKey.ExpireDate -> getString(R.string.merge_field_expire_date)
      FieldKey.Expires -> getString(R.string.merge_field_expires)
      FieldKey.LegacyItem -> getString(R.string.merge_field_whole_item)
    }
  }

  private fun fieldValueText(value: FieldValue, renderer: ConflictRenderer): String {
    return when (renderer) {
      ConflictRenderer.PROTECTED_STRING -> (value as? FieldValue.StringValue)?.let {
        protectedStringText(it.value)
      } ?: fallbackFieldValueText(value)
      ConflictRenderer.BINARY -> (value as? FieldValue.BinaryValue)?.let { binaryText(it) }
        ?: fallbackFieldValueText(value)
      ConflictRenderer.TEXT -> fallbackFieldValueText(value)
      ConflictRenderer.ICON -> (value as? FieldValue.IconValue)?.let {
        iconText(it.value)
      } ?: fallbackFieldValueText(value)
      ConflictRenderer.CUSTOM_ICON -> (value as? FieldValue.CustomIconValue)?.let {
        customIconText(it.value)
      } ?: fallbackFieldValueText(value)
      ConflictRenderer.DATE -> when (value) {
        is FieldValue.DateValue -> dateText(value.value)
        is FieldValue.NullableDateValue -> value.value?.let { dateText(it) }.orEmpty()
        else -> fallbackFieldValueText(value)
      }
      ConflictRenderer.BOOLEAN -> (value as? FieldValue.BooleanValue)?.let {
        if (it.value) getString(R.string.yes) else getString(R.string.no)
      } ?: fallbackFieldValueText(value)
      ConflictRenderer.ENTRY_PROPERTY -> (value as? FieldValue.PropertyValue)?.let {
        entryPropertyValueText(it.value)
      } ?: fallbackFieldValueText(value)
      ConflictRenderer.GROUP_PROPERTY -> (value as? FieldValue.PropertyValue)?.let {
        entryPropertyValueText(it.value)
      } ?: fallbackFieldValueText(value)
      ConflictRenderer.LEGACY_ITEM -> (value as? FieldValue.LegacyItemValue)?.let {
        legacyItemText(it.value)
      } ?: fallbackFieldValueText(value)
    }
  }

  private fun fallbackFieldValueText(value: FieldValue): String {
    return when (value) {
      is FieldValue.StringValue -> protectedStringText(value.value)
      is FieldValue.BinaryValue -> binaryText(value)
      is FieldValue.TextValue -> value.value
      is FieldValue.IconValue -> iconText(value.value)
      is FieldValue.CustomIconValue -> customIconText(value.value)
      is FieldValue.TagsValue -> value.value
      is FieldValue.DateValue -> dateText(value.value)
      is FieldValue.NullableDateValue -> value.value?.let { dateText(it) }.orEmpty()
      is FieldValue.BooleanValue -> if (value.value) getString(R.string.yes) else getString(R.string.no)
      is FieldValue.PropertyValue -> entryPropertyValueText(value.value)
      is FieldValue.LegacyItemValue -> legacyItemText(value.value)
    }
  }

  private fun legacyItemText(item: Any): String {
    return when (item) {
      is PwEntry -> getString(
        R.string.merge_legacy_entry_summary,
        item.title.orEmpty(),
        item.username.orEmpty(),
        item.password.orEmpty(),
        item.url.orEmpty(),
        item.notes.orEmpty(),
        dateText(item.lastModificationTime)
      )
      is PwGroup -> getString(
        R.string.merge_legacy_group_summary,
        item.name.orEmpty(),
        dateText(item.lastMod)
      )
      else -> item.toString()
    }
  }

  private fun entryPropertyLabel(property: EntryProperty): String {
    return when (property) {
      EntryProperty.FOREGROUND_COLOR -> getString(R.string.merge_field_foreground_color)
      EntryProperty.BACKGROUND_COLOR -> getString(R.string.merge_field_background_color)
      EntryProperty.OVERRIDE_URL -> getString(R.string.merge_field_override_url)
      EntryProperty.AUTO_TYPE -> getString(R.string.merge_field_auto_type)
      EntryProperty.LOCATION_CHANGED -> getString(R.string.merge_field_location_changed)
      EntryProperty.CREATION_TIME -> getString(R.string.merge_field_creation_time)
      EntryProperty.LAST_MODIFICATION_TIME -> getString(R.string.merge_field_last_modification_time)
      EntryProperty.LAST_ACCESS_TIME -> getString(R.string.merge_field_last_access_time)
      EntryProperty.USAGE_COUNT -> getString(R.string.merge_field_usage_count)
      EntryProperty.INTERNAL_URL -> getString(R.string.merge_field_internal_url)
      EntryProperty.ADDITIONAL -> getString(R.string.merge_field_additional)
      EntryProperty.CUSTOM_DATA -> getString(R.string.merge_field_custom_data)
      EntryProperty.PREVIOUS_PARENT_GROUP -> getString(R.string.merge_field_previous_parent_group)
      EntryProperty.QUALITY_CHECK -> getString(R.string.merge_field_quality_check)
    }
  }

  private fun groupPropertyLabel(property: GroupProperty): String {
    return when (property) {
      GroupProperty.IS_EXPANDED -> getString(R.string.merge_field_group_is_expanded)
      GroupProperty.DEFAULT_AUTO_TYPE_SEQUENCE -> getString(R.string.merge_field_group_default_auto_type_sequence)
      GroupProperty.ENABLE_AUTO_TYPE -> getString(R.string.merge_field_group_enable_auto_type)
      GroupProperty.ENABLE_SEARCHING -> getString(R.string.merge_field_group_enable_searching)
      GroupProperty.LAST_TOP_VISIBLE_ENTRY -> getString(R.string.merge_field_group_last_top_visible_entry)
      GroupProperty.LOCATION_CHANGED -> getString(R.string.merge_field_location_changed)
      GroupProperty.CREATION_TIME -> getString(R.string.merge_field_creation_time)
      GroupProperty.LAST_MODIFICATION_TIME -> getString(R.string.merge_field_last_modification_time)
      GroupProperty.LAST_ACCESS_TIME -> getString(R.string.merge_field_last_access_time)
      GroupProperty.USAGE_COUNT -> getString(R.string.merge_field_usage_count)
      GroupProperty.PREVIOUS_PARENT_GROUP -> getString(R.string.merge_field_previous_parent_group)
      GroupProperty.CUSTOM_DATA -> getString(R.string.merge_field_custom_data)
    }
  }

  private fun entryPropertyValueText(value: EntryPropertyValue): String {
    return when (value) {
      is EntryPropertyValue.Text -> value.value.orEmpty()
      is EntryPropertyValue.BooleanFlag -> if (value.value) getString(R.string.yes) else getString(R.string.no)
      is EntryPropertyValue.NullableBooleanFlag -> value.value?.let {
        if (it) getString(R.string.yes) else getString(R.string.no)
      }.orEmpty()
      is EntryPropertyValue.LongNumber -> value.value.toString()
      is EntryPropertyValue.UuidValue -> value.value?.toString().orEmpty()
      is EntryPropertyValue.DateTime -> value.value?.let { dateText(it) }.orEmpty()
      is EntryPropertyValue.AutoType -> listOf(
        if (value.enabled) getString(R.string.yes) else getString(R.string.no),
        value.defaultSequence,
        value.obfuscationOptions.toString(),
        value.windowSequencePairs.entries.joinToString("\n") { "${it.key}: ${it.value}" }
      ).filter { it.isNotBlank() }.joinToString("\n")
      is EntryPropertyValue.CustomData -> value.values.entries.joinToString("\n") { "${it.key}: ${it.value}" }
    }
  }

  private fun protectedStringText(value: ProtectedString): String = value.toString()

  private fun binaryText(value: FieldValue.BinaryValue): String {
    return if (value.name.isBlank()) {
      getString(R.string.merge_binary_summary, value.value.length())
    } else {
      getString(R.string.merge_binary_named_summary, value.name, value.value.length())
    }
  }

  private fun iconText(value: PwIconStandard): String {
    return getString(R.string.merge_icon_summary, value.iconId)
  }

  private fun customIconText(value: PwIconCustom?): String {
    return value?.uuid?.toString().orEmpty()
  }

  private fun dateText(value: Date): String {
    return DateFormat.getDateTimeInstance().format(value)
  }

  private inner class ConflictFieldAdapter(
    private val itemIndex: Int,
    private val conflicts: List<FieldConflict>
  ) : RecyclerView.Adapter<ConflictFieldAdapter.ConflictFieldHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConflictFieldHolder {
      val itemBinding = ItemMergeConflictFieldBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
      )
      return ConflictFieldHolder(itemBinding)
    }

    override fun getItemCount(): Int = conflicts.size

    override fun onBindViewHolder(holder: ConflictFieldHolder, position: Int) {
      holder.bind(conflicts[position], position)
    }

    private inner class ConflictFieldHolder(
      private val itemBinding: ItemMergeConflictFieldBinding
    ) : RecyclerView.ViewHolder(itemBinding.root) {

      fun bind(conflict: FieldConflict, position: Int) = with(itemBinding) {
        fieldName.text = fieldLabel(conflict.key)
        cloudValue.text = fieldValueText(conflict.cloud, conflict.renderer)
        localValue.text = fieldValueText(conflict.local, conflict.renderer)
        val selected = model.decisions[itemIndex]?.get(conflict.key)
        resultValue.text = selectedValueText(conflict, selected)
        applyChoiceState(cloudValue, resultValue, localValue, selected)
        cloudValue.setOnClickListener { choose(conflict, Decision.CLOUD, position) }
        localValue.setOnClickListener { choose(conflict, Decision.LOCAL, position) }
      }
    }

    private fun choose(conflict: FieldConflict, decision: Decision, position: Int) {
      model.decisions.getOrPut(itemIndex) { linkedMapOf() }[conflict.key] = decision
      notifyItemChanged(position)
    }
  }

  private fun selectedValueText(conflict: FieldConflict, selected: Decision?): String {
    return when (selected) {
      Decision.LOCAL -> fieldValueText(conflict.local, conflict.renderer)
      Decision.CLOUD -> fieldValueText(conflict.cloud, conflict.renderer)
      null -> getString(R.string.merge_not_selected)
    }
  }

  private fun applyChoiceState(
    cloudValue: AppCompatTextView,
    resultValue: AppCompatTextView,
    localValue: AppCompatTextView,
    selected: Decision?
  ) {
    cloudValue.setBackgroundResource(
      if (selected == Decision.CLOUD) R.drawable.bg_merge_selected else R.drawable.bg_ripple_white_selector
    )
    localValue.setBackgroundResource(
      if (selected == Decision.LOCAL) R.drawable.bg_merge_selected else R.drawable.bg_ripple_white_selector
    )
    resultValue.setBackgroundResource(
      if (selected == null) R.drawable.bg_gray_radius_4 else R.drawable.bg_merge_selected
    )
  }

  private inner class LocalOnlyAdapter : RecyclerView.Adapter<LocalOnlyAdapter.LocalOnlyHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocalOnlyHolder {
      val itemBinding = ItemMergeLocalOnlyBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
      )
      return LocalOnlyHolder(itemBinding)
    }

    override fun getItemCount(): Int = model.session.localOnlyItems.size

    override fun onBindViewHolder(holder: LocalOnlyHolder, position: Int) {
      holder.bind(position)
    }

    private inner class LocalOnlyHolder(
      private val itemBinding: ItemMergeLocalOnlyBinding
    ) : RecyclerView.ViewHolder(itemBinding.root) {

      fun bind(position: Int) = with(itemBinding) {
        localOnlyTitle.text = itemTitle(model.session.localOnlyItems[position])
        localOnlyCheck.isChecked = position in model.deleteLocalOnlyIndexes
        root.setOnClickListener {
          if (position in model.deleteLocalOnlyIndexes) {
            model.deleteLocalOnlyIndexes -= position
          } else {
            model.deleteLocalOnlyIndexes += position
          }
          notifyItemChanged(position)
        }
      }
    }
  }

  companion object {
    const val ROUTE = "/activity/merge_conflict"
    const val KEY_SESSION_ID = "sessionId"
    private const val LANDSCAPE_DESIGN_WIDTH_DP = 731f
    private const val COMPLETION_LOADING_DIALOG_TAG = "merge_completion_loading"
  }
}
