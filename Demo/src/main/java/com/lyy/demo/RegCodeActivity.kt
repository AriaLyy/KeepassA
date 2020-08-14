package com.lyy.demo

import android.os.Bundle
import android.view.View
import com.arialyy.frame.core.AbsActivity
import com.lyy.demo.databinding.ActivityRegCodeBinding

class RegCodeActivity : AbsActivity<ActivityRegCodeBinding>() {
  val userName = "lyy222"

  override fun setLayoutId(): Int {
    return R.layout.activity_reg_code
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    binding.tvUser.append(userName)

  }

  fun onClick(v: View) {
    when (v.id) {
      R.id.btCreateCode -> {

      }
      R.id.btVerifyCode -> {

      }
    }

  }
}