package com.lyy.demo

import android.content.Intent
import android.os.Bundle
import com.arialyy.frame.core.AbsActivity
import com.lyy.demo.databinding.ActivityLoginTestBinding

class LoginActivity:AbsActivity<ActivityLoginTestBinding>() {
  override fun setLayoutId(): Int {
    return R.layout.activity_login_test
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    binding.btLogin.setOnClickListener {
      val userName = binding.etUser.text.toString()
      val pass = binding.etPass.text.toString()
      startActivity(Intent(this, MainActivity::class.java))
      finish()
    }
  }


}