package com.lyy.keepassa.service.play

import android.app.Activity
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.ToastUtils
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManagerFactory
import com.lyy.keepassa.R
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 14:46 2024/5/6
 **/
class ReviewUtil(val context: Activity) {
  private val manager = ReviewManagerFactory.create(context)

  fun startReview() {
    manager.requestReviewFlow().addOnCompleteListener {
      if (it.isSuccessful) {
        val reviewInfo = it.result
        Timber.d("start rate")
        manager.launchReviewFlow(context, reviewInfo).addOnCompleteListener { reviewFlow ->
          // 评价成功后的操作
          Timber.d("${reviewFlow.isSuccessful}")
        }
        return@addOnCompleteListener
      }
      Timber.e("start review fail, errorCode: ${(it.exception as ReviewException).errorCode}")
      ToastUtils.showLong(ResUtil.getString(R.string.review_fail))
    }
  }
}