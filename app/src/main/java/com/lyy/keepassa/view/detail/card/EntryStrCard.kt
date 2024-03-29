package com.lyy.keepassa.view.detail.card

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.text.InputType
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.arialyy.frame.util.ResUtil
import com.google.android.material.card.MaterialCardView
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.base.AbsViewBindingAdapter
import com.lyy.keepassa.base.KeyConstance
import com.lyy.keepassa.databinding.LayoutEntryCardListBinding
import com.lyy.keepassa.databinding.LayoutEntryStrBinding
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.doClick
import com.lyy.keepassa.util.doOnItemClickListener
import com.lyy.keepassa.util.hasTOTP
import com.lyy.keepassa.util.totp.OtpUtil
import com.lyy.keepassa.view.menu.EntryDetailStrPopMenu
import com.lyy.keepassa.view.menu.EntryDetailStrPopMenu.OnShowPassCallback
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

    if (entry.hasTOTP()) {
      val totpPass = OtpUtil.getOtpPass(entry)
      if (!TextUtils.isEmpty(totpPass.second)) {
        val totpPassStr = ProtectedString(true, totpPass.second)
        totpPassStr.isOtpPass = true
        data.add(object : Entry<String, ProtectedString> {
          override val key: String
            get() = KeyConstance.TOTP
          override val value: ProtectedString
            get() = totpPassStr
        })
      }
    }
    data.sortBy { it.key }

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
      adapter.setData(data)
      isNestedScrollingEnabled = false
    }
    binding.rvList.doOnItemClickListener { _, position, view ->
      val tvValue = view.findViewById<TextView>(R.id.value)
      val entry = data[position]
      if (entry.value.toString().isEmpty()) {
        Timber.e("value is null")
        return@doOnItemClickListener
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
    AbsViewBindingAdapter<Entry<String, ProtectedString>, LayoutEntryStrBinding>() {

    private fun handleOtp(binding: LayoutEntryStrBinding, tvValue: TextView) {
      binding.rpbBar.visibility = VISIBLE
      KdbUtil.startAutoGetOtp(entryV4, binding.rpbBar, tvValue)
      binding.ivEye.isVisible = true
      binding.ivEye.isSelected = true
      binding.ivEye.doClick {
        binding.ivEye.isSelected = !binding.ivEye.isSelected
        KpaUtil.handleShowPass(binding.value, !binding.ivEye.isSelected)
      }
    }

    @SuppressLint("SetTextI18n")
    override fun bindData(binding: LayoutEntryStrBinding, item: Entry<String, ProtectedString>) {
      binding.title.text = item.key
      val tvValue = binding.value
      KpaUtil.handleShowPass(tvValue, !item.value.isProtected)
      if (item.value.isOtpPass) {
        handleOtp(binding, tvValue)
        return
      }
      binding.ivEye.isVisible = false
      binding.rpbBar.isVisible = false
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
  }
}