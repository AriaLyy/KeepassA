package com.lyy.keepassa.service.play

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.SkuType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.SkuDetailsResult
import com.android.billingclient.api.querySkuDetails
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.ToastUtils
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.view.dialog.LoadingDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * https://developer.android.com/google/play/billing/integrate?hl=zh-cn#fetch
 * @Author laoyuyu
 * @Description
 * @Date 1:58 下午 2022/1/20
 **/
class PlayServiceUtil {
  private val scope = MainScope()
  private var isConnected = false
  private val loadingDialog = LoadingDialog(BaseApp.APP)

  /**
   * 购买回调
   */
  private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
    // To be implemented in a later section.
    Timber.d("result = ${billingResult}, purchases = $purchases")
    if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
      Timber.d("purchases success")
      for (purchase in purchases) {
        handlePurchase(purchase)
      }
    } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
      Timber.d("cancel")
      // Handle an error caused by a user cancelling the purchase flow.
    } else {
      // Handle any other error codes.
    }
  }

  private var billingClient = BillingClient.newBuilder(BaseApp.APP)
    .setListener(purchasesUpdatedListener)
    .enablePendingPurchases()
    .build()

  /**
   * 确认非消耗型商品的购买交易
   */
  private fun handlePurchase(purchase: Purchase) {
    if (purchase.purchaseState == PurchaseState.PURCHASED) {
      if (!purchase.isAcknowledged) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
          .setPurchaseToken(purchase.purchaseToken)
        billingClient.acknowledgePurchase(acknowledgePurchaseParams.build()) {

          Timber.d("code = ${it.responseCode}")
        }
      }
    }
  }

  /**
   * connect to play
   */
  fun connectPlay(callback: (Boolean) -> Unit) {
    // loadingDialog.show()
    billingClient.startConnection(object : BillingClientStateListener {
      override fun onBillingSetupFinished(billingResult: BillingResult) {
        // loadingDialog.dismiss()
        if (billingResult.responseCode == BillingResponseCode.OK) {
          // The BillingClient is ready. You can query purchases here.
          isConnected = true
          callback.invoke(true)
          return
        }
        ToastUtils.showLong(ResUtil.getString(R.string.error_connect_play))
        callback.invoke(false)
      }

      override fun onBillingServiceDisconnected() {
        // Try to restart the connection on the next request to
        // Google Play by calling the startConnection() method.
        isConnected = false
        loadingDialog.dismiss()
      }
    })
  }

  /**
   * 普通商品
   */
  suspend fun queryInappSkuDetails(): SkuDetailsResult {
    val skuList = ArrayList<String>()
    skuList.add("01_rqvz3usue52wu77d7dqhywfjc23ki9ae")
    skuList.add("02_i5phiiuoccqxrwzhu72caqpfjz2ayrsx")
    skuList.add("03_9i7njrgcrr4rev554hnz777a7uhxxy2w")
    val params = SkuDetailsParams.newBuilder()
    params.setSkusList(skuList).setType(SkuType.INAPP)

    // leverage querySkuDetails Kotlin extension function
    return withContext(Dispatchers.IO) {
      val detail = billingClient.querySkuDetails(params.build())
      val skuList = detail.skuDetailsList
      val skuResult = detail.billingResult
      Timber.d("get inapp sku success, size = ${skuList?.size}")
      return@withContext detail
    }

    // Process the result.
  }

  /**
   * 检查是否已订阅
   * @return true 已订阅
   */
  suspend fun isSubscribed(): Boolean {

    val d = withContext(Dispatchers.IO) {
      return@withContext billingClient.queryPurchases(SkuType.SUBS)
    }
    if (d.responseCode != BillingResponseCode.OK) {
      Timber.d("get history error")
      return false
    }

    d.purchasesList?.forEach {
      if (it.isAutoRenewing) {
        return true
      }
    }
    return false
  }

  /**
   * 订阅商品
   */
  suspend fun querySubSkuDetails(): SkuDetailsResult {
    val skuList = ArrayList<String>()
    skuList.add("11_wdvkaaymasi93y7r5hnrcm7unccqv2a7")
    skuList.add("21_rgwbquffzynsq23ca3239npc7r7fddnm")
    skuList.add("14_v9bgagya5emfagnbxizhmeoj223wfsuq")
    skuList.add("13_w2vn9xn7k9zqga29jrxndrtut4kifipo")
    val params = SkuDetailsParams.newBuilder()
    params.setSkusList(skuList).setType(SkuType.SUBS)

    // leverage querySkuDetails Kotlin extension function
    return withContext(Dispatchers.IO) {
      val detail = billingClient.querySkuDetails(params.build())
      val skuList = detail.skuDetailsList
      val skuResult = detail.billingResult
      Timber.d("get sub sku success, size = ${skuList?.size}")
      return@withContext detail
    }
  }

  /**
   * 开始支付流程
   */
  suspend fun startPlayFlow(ac: Activity, skuDetail: SkuDetails) {
    val flowParams = BillingFlowParams.newBuilder()
      .setSkuDetails(skuDetail)
      .build()
    val responseCode = billingClient.launchBillingFlow(ac, flowParams).responseCode
  }

  fun onDestroy() {
    billingClient.endConnection()
  }
}