package com.lyy.keepassa.view.detail.card

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.arialyy.frame.util.ResUtil
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.card.MaterialCardView
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedBinary
import com.lyy.keepassa.R
import com.lyy.keepassa.databinding.LayoutEntryCardListBinding
import com.lyy.keepassa.view.menu.EntryDetailFilePopMenu

/**
 * @Author laoyuyu
 * @Description
 * @Date 11:12 AM 2023/9/26
 **/
class EntryAttachmentCard(context: Context, attributeSet: AttributeSet) :
  MaterialCardView(context, attributeSet) {
  private val binding = LayoutEntryCardListBinding.inflate(LayoutInflater.from(context), this, true)

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

  private fun handleList(data: MutableList<MutableMap.MutableEntry<String, ProtectedBinary>>) {
    val adapter = AttachmentAdapter()

    binding.rvList.apply {
      this.adapter = adapter
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context)
      adapter.setNewInstance(data)
    }
    adapter.setOnItemClickListener { _, view, position ->
      val entry = data[position]
      val pop = EntryDetailFilePopMenu(context as FragmentActivity, view, entry.key, entry.value)

      pop.show()
    }
  }

  private class AttachmentAdapter :
    BaseQuickAdapter<MutableMap.MutableEntry<String, ProtectedBinary>, BaseViewHolder>(R.layout.layout_entry_attachment) {
    override fun convert(
      holder: BaseViewHolder,
      item: MutableMap.MutableEntry<String, ProtectedBinary>
    ) {
      holder.setText(R.id.value, item.key)
    }
  }
}