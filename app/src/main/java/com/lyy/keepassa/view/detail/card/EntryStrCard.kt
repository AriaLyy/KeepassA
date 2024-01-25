package com.lyy.keepassa.view.detail.card

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.text.InputType
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.arialyy.frame.util.ResUtil
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.card.MaterialCardView
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.databinding.LayoutEntryCardListBinding
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.view.menu.EntryDetailStrPopMenu
import com.lyy.keepassa.view.menu.EntryDetailStrPopMenu.OnShowPassCallback
import com.lyy.keepassa.widget.ProgressBar.RoundProgressBarWidthNumber
import timber.log.Timber
import kotlin.collections.Map.Entry

/**
 * @Author laoyuyu
 * @Description
 * @Date 11:12 AM 2023/9/26
 **/
class EntryStrCard(context: Context, attributeSet: AttributeSet) :
  MaterialCardView(context, attributeSet) {
  private val binding = LayoutEntryCardListBinding.inflate(LayoutInflater.from(context), this, true)

  fun bindData(entry: PwEntryV4) {
    binding.tvCardTitle.text = ResUtil.getString(R.string.hint_attr)
    val data = KdbUtil.filterCustomStr(entry).entries.toMutableList()
    if (data.isEmpty()) {
      visibility = GONE
      return
    }
    visibility = VISIBLE
    handleList(entry, data)
  }

  private fun handleList(entryV4: PwEntryV4, data: MutableList<Entry<String, ProtectedString>>) {
    val adapter = StrAdapter(entryV4)

    binding.rvList.apply {
      this.adapter = adapter
      setHasFixedSize(true)
      layoutManager = object : LinearLayoutManager(context) {
        override fun canScrollVertically(): Boolean {
          return false
        }
      }
      adapter.setNewInstance(data)
      isNestedScrollingEnabled = false
    }
    adapter.setOnItemClickListener { _, view, position ->
      val tvValue = view.findViewById<TextView>(R.id.value)
      val entry = data[position]
      if (entry.value.toString().isEmpty()) {
        Timber.e("value is null")
        return@setOnItemClickListener
      }
      val pop = EntryDetailStrPopMenu(
        context as FragmentActivity,
        view,
        entry.value,
        tvValue.inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
      )
      pop.setOnShowPassCallback(object : OnShowPassCallback {
        override fun showPass(showPass: Boolean) {
          KpaUtil.handleShowPass(tvValue, showPass)
        }
      })
      pop.show()
    }
  }

  private class StrAdapter(val entryV4: PwEntryV4) :
    BaseQuickAdapter<Entry<String, ProtectedString>, BaseViewHolder>(R.layout.layout_entry_str) {
    @SuppressLint("SetTextI18n")
    override fun convert(holder: BaseViewHolder, item: Entry<String, ProtectedString>) {
      holder.setText(R.id.title, item.key)
      val tvValue = holder.getView<TextView>(R.id.value)
      KpaUtil.handleShowPass(tvValue, !item.value.isProtected)
      if (item.value.isOtpPass) {
        handleOtp(holder, tvValue)
        return
      }
      holder.setGone(R.id.rpbBar, true)
      if (item.value.toString().isEmpty()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          tvValue.typeface = context.resources.getFont(R.font.roboto_thinitalic)
        }
        tvValue.text = "null"
        return
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        tvValue.typeface = context.resources.getFont(R.font.roboto_regular)
      }
      tvValue.text = item.value.toString()
    }

    private fun handleOtp(holder: BaseViewHolder, tvValue: TextView) {
      val rPb = holder.getView<RoundProgressBarWidthNumber>(R.id.rpbBar)
      rPb.visibility = VISIBLE
      KdbUtil.startAutoGetOtp(entryV4, rPb, tvValue)
    }
  }
}