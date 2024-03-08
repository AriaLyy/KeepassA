package com.lyy.keepassa.view.detail.card

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.arialyy.frame.util.ResUtil
import com.google.android.material.card.MaterialCardView
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedBinary
import com.lyy.keepassa.R
import com.lyy.keepassa.base.AbsViewBindingAdapter
import com.lyy.keepassa.databinding.LayoutEntryAttachmentBinding
import com.lyy.keepassa.databinding.LayoutEntryCardListBinding
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.doOnItemClickListener
import com.lyy.keepassa.view.menu.EntryDetailFilePopMenu
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlin.collections.MutableMap.MutableEntry

/**
 * @Author laoyuyu
 * @Description
 * @Date 11:12 AM 2023/9/26
 **/
class EntryFileCard(context: Context, attributeSet: AttributeSet) :
  MaterialCardView(context, attributeSet) {
  private val binding = LayoutEntryCardListBinding.inflate(LayoutInflater.from(context), this, true)

  companion object {
    val SAVE_FILE_FLOW = MutableSharedFlow<Pair<String, ProtectedBinary>>()
  }

  fun bindData(entry: PwEntryV4) {
    binding.tvCardTitle.text = ResUtil.getString(R.string.attachment)
    val data = entry.binaries.entries.toMutableList()
    if (data.isEmpty()) {
      visibility = GONE
      return
    }
    visibility = VISIBLE
    handleList(data)
  }

  private fun handleList(data: MutableList<MutableEntry<String, ProtectedBinary>>) {
    val adapter = AttachmentAdapter()

    binding.rvList.apply {
      this.adapter = adapter
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context)
      adapter.setData(data)
    }
    binding.rvList.doOnItemClickListener { _, position, v ->
      val entry = data[position]
      val pop = EntryDetailFilePopMenu(context as FragmentActivity, v, entry.key, entry.value)
      pop.setOnDownloadClick(object : EntryDetailFilePopMenu.OnDownloadClick {
        override fun onDownload(key: String, file: ProtectedBinary) {
          KpaUtil.scope.launch {
            SAVE_FILE_FLOW.emit(Pair(key, file))
          }
        }
      })
      pop.show()
    }
  }

  private class AttachmentAdapter :
    AbsViewBindingAdapter<MutableEntry<String, ProtectedBinary>, LayoutEntryAttachmentBinding>() {

    override fun bindData(
      binding: LayoutEntryAttachmentBinding,
      item: MutableEntry<String, ProtectedBinary>
    ) {
      binding.value.text = item.key
    }
  }
}