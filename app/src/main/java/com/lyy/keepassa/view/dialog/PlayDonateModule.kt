// /*
//  * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
//  *
//  * This Source Code Form is subject to the terms of the Mozilla Public
//  * License, v. 2.0. If a copy of the MPL was not distributed with this
//  * file, you can obtain one at http://mozilla.org/MPL/2.0/.
//  */
// package com.lyy.keepassa.view.dialog
//
// import android.app.Activity
// import androidx.lifecycle.viewModelScope
// import com.android.billingclient.api.BillingClient
// import com.android.billingclient.api.BillingClient.BillingResponseCode
// import com.android.billingclient.api.BillingClient.SkuType
// import com.android.billingclient.api.BillingClientStateListener
// import com.android.billingclient.api.BillingFlowParams
// import com.android.billingclient.api.BillingResult
// import com.android.billingclient.api.ConsumeParams
// import com.android.billingclient.api.Purchase
// import com.android.billingclient.api.Purchase.PurchaseState
// import com.android.billingclient.api.PurchasesUpdatedListener
// import com.android.billingclient.api.SkuDetails
// import com.android.billingclient.api.SkuDetailsParams
// import com.android.billingclient.api.SkuDetailsResult
// import com.android.billingclient.api.consumePurchase
// import com.android.billingclient.api.querySkuDetails
// import com.arialyy.frame.util.ResUtil
// import com.blankj.utilcode.util.ToastUtils
// import com.lyy.keepassa.R
// import com.lyy.keepassa.base.BaseApp
// import com.lyy.keepassa.base.BaseModule
// import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.flow.MutableStateFlow
// import kotlinx.coroutines.launch
// import kotlinx.coroutines.withContext
// import timber.log.Timber
//
// /**
//  * @Author laoyuyu
//  * @Description https://developer.android.com/google/play/billing/integrate?hl=zh-cn#fetch
//  * @Date 2:32 下午 2022/2/7
//  **/
// class PlayDonateModule : BaseModule() {
//
//   companion object{
//     const val STATE_CONNECT_PLAY_SERVICE_ERROR = -1
//     const val STATE_DONATE_SUCCESS = 0
//     const val STATE_DONATE_FAIL = 1
//     const val STATE_DEFAULT = 999
//   }
//
//   private var isConnected = false
//   private var skuResult: SkuDetailsResult? = null
//   var curIndex = 1f
//   val playFlow = MutableStateFlow(STATE_DEFAULT)
//
//   private val skuList = arrayListOf<String>().apply {
//     add("d1_cddobn39u5ugvvvsn2fqvd5ktzfhnxqr")
//     add("d5_wgi5ukrzfj4259m77s2ymn2fkzy5cvgc")
//     add("d10_5h35r3juggi7pwyiy4mqi7zgbehd9kdb")
//     add("d20_wswi2fefwoaswb9u9bmmk35vhd9rhpsy")
//     add("d50_942stx4ouh4dk7suz7j9yrvjpwtwi9np")
//   }
//
//   private val skuDetailMap = hashMapOf<String, SkuDetails>()
//
//   /**
//    * 购买回调
//    */
//   private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
//     // To be implemented in a later section.
//     Timber.d("result = ${billingResult}, purchases = $purchases")
//     if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
//       Timber.d("purchases success")
//       for (purchase in purchases) {
//         handlePurchase(purchase)
//       }
//     } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
//       Timber.d("cancel")
//       // Handle an error caused by a user cancelling the purchase flow.
//     } else {
//       // Handle any other error codes.
//     }
//   }
//
//   private var billingClient = BillingClient.newBuilder(BaseApp.APP)
//     .setListener(purchasesUpdatedListener)
//     .enablePendingPurchases()
//     .build()
//
//   /**
//    * 确认非消耗型商品的购买交易
//    */
//   private fun handlePurchase(purchase: Purchase) {
//     if (purchase.purchaseState == PurchaseState.PURCHASED) {
//       // if (!purchase.isAcknowledged) {
//       //   val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
//       //     .setPurchaseToken(purchase.purchaseToken)
//       //   billingClient.acknowledgePurchase(acknowledgePurchaseParams.build()) {
//       //     Timber.d("code = ${it.responseCode}")
//       //   }
//       // }
//
//       val consumeParams =
//         ConsumeParams.newBuilder()
//           .setPurchaseToken(purchase.purchaseToken)
//           .build()
//       viewModelScope.launch {
//         val consumeResult = withContext(Dispatchers.IO) {
//           billingClient.consumePurchase(consumeParams)
//         }
//         playFlow.emit(STATE_DONATE_SUCCESS)
//       }
//     }
//   }
//
//   /**
//    * 1、查询sku
//    * 2、进行支付
//    */
//   fun startFlow(ac: Activity, value: Float) {
//     if (isConnected) {
//       startQuerySdk(ac, value)
//       return
//     }
//     connectPlay {
//       if (!it){
//         viewModelScope.launch {
//           playFlow.emit(STATE_CONNECT_PLAY_SERVICE_ERROR)
//         }
//         return@connectPlay
//       }
//       if (it) {
//         startQuerySdk(ac, value)
//       }
//     }
//   }
//
//   private fun startQuerySdk(ac: Activity, value: Float) {
//     viewModelScope.launch {
//       queryInappSkuDetails()
//       val curSku = skuList[value.toInt() - 1]
//       Timber.d("curSku = $curSku")
//       val skuDetail = skuDetailMap[curSku]
//       if (skuDetail == null) {
//         playFlow.emit(STATE_DONATE_FAIL)
//         return@launch
//       }
//       startPlayFlow(ac, skuDetail)
//     }
//   }
//
//   /**
//    * 开始支付流程
//    */
//   private fun startPlayFlow(ac: Activity, skuDetail: SkuDetails) {
//     val flowParams = BillingFlowParams.newBuilder()
//       .setSkuDetails(skuDetail)
//       .build()
//     val responseCode = billingClient.launchBillingFlow(ac, flowParams).responseCode
//     if (responseCode != BillingResponseCode.OK){
//       viewModelScope.launch {
//         playFlow.emit(STATE_DONATE_FAIL)
//       }
//     }
//     Timber.d("responseCode = $responseCode")
//   }
//
//   /**
//    * connect to play
//    */
//   private fun connectPlay(callback: (Boolean) -> Unit) {
//     billingClient.startConnection(object : BillingClientStateListener {
//       override fun onBillingSetupFinished(billingResult: BillingResult) {
//         if (billingResult.responseCode == BillingResponseCode.OK) {
//           // The BillingClient is ready. You can query purchases here.
//           isConnected = true
//           callback.invoke(true)
//           return
//         }
//         ToastUtils.showLong(ResUtil.getString(R.string.error_connect_play))
//         callback.invoke(false)
//       }
//
//       override fun onBillingServiceDisconnected() {
//         // Try to restart the connection on the next request to
//         // Google Play by calling the startConnection() method.
//         isConnected = false
//       }
//     })
//   }
//
//   /**
//    * 普通商品
//    */
//   suspend fun queryInappSkuDetails(): SkuDetailsResult? {
//     if (skuResult != null) {
//       return skuResult
//     }
//     val params = SkuDetailsParams.newBuilder()
//     params.setSkusList(skuList).setType(SkuType.INAPP)
//
//     // leverage querySkuDetails Kotlin extension function
//     return withContext(Dispatchers.IO) {
//       skuResult = billingClient.querySkuDetails(params.build())
//       val skuList = skuResult?.skuDetailsList
//       skuList?.forEach {
//         skuDetailMap[it.sku] = it
//       }
//       // val skuResult = skuResult.billingResult
//       Timber.d("get inapp sku success, size = ${skuList?.size}")
//       return@withContext skuResult
//     }
//
//     // Process the result.
//   }
//
//   fun convertValue(value: Float): String {
//     return when (value.toInt()) {
//       1 -> {
//         "$1"
//       }
//       2 -> {
//         "$5"
//       }
//       3 -> {
//         "$10"
//       }
//       4 -> {
//         "$20"
//       }
//       5 -> {
//         "$50"
//       }
//       else -> {
//         "$1"
//       }
//     }
//   }
//
//   override fun onCleared() {
//     super.onCleared()
//     billingClient.endConnection()
//   }
// }