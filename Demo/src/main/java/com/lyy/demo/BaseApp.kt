package com.lyy.demo

import android.app.Application
import com.arialyy.frame.core.AbsFrame

class BaseApp:Application() {

  override fun onCreate() {
    super.onCreate()
    AbsFrame.init(this)
  }
}