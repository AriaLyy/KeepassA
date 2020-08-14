package com.lyy.keepassa.view.fingerprint

import android.annotation.SuppressLint
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseFragment
import com.lyy.keepassa.databinding.FragmentFingerprintCloseBinding

/**
 * 指纹关闭展示页面
 */
class FingerprintCloseFragment : BaseFragment<FragmentFingerprintCloseBinding>() {
  @SuppressLint("SetTextI18n")
  override fun initData() {
    binding.tvHint.text = "${getString(R.string.fingerprint_unlock)}${getString(R.string.closed)}"
  }

  override fun setLayoutId(): Int {
    return R.layout.fragment_fingerprint_close
  }
}