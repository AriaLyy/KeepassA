package com.lyy.keepassa.service.play

import android.app.Activity
import android.content.Context
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.facade.template.IProvider

/**
 * @Author laoyuyu
 * @Description
 * @Date 14:44 2024/5/6
 **/
@Route(path = "/service/play")
class PlayerService : IProvider {

  /**
   * 应用内评价
   */
  fun review(ac: Activity) {
    ReviewUtil(ac).startReview()
  }

  override fun init(context: Context?) {
  }
}