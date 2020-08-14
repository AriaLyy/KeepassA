package com.lyy.playimp

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.SkuType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.querySkuDetails
import com.lyy.ibaseapi.BillingParam
import com.lyy.ibaseapi.GoodsEntity
import com.lyy.ibaseapi.IPlay
import com.lyy.ibaseapi.SkuType.INAPP
import java.io.IOException
import java.lang.Exception
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.locks.ReentrantLock

/**
 * https://juejin.im/post/5d402aee5188255d53594824
 * 流程：
 * 1、查询商品库存
 * 2、支付
 * 3、消耗商品
 *
 */
class PlayPay : IPlay, PurchasesUpdatedListener, BillingClientStateListener {
  private var billingClient: BillingClient? = null
  private var skuDetails: MutableList<SkuDetails> = ArrayList()
  private val lock = ReentrantLock()
  private val condition = lock.newCondition()

  companion object {
    private val TAG = PlayPay::class.simpleName

    @Volatile
    private var INSTANCE: PlayPay? = null

    fun getInstance(): PlayPay =
      INSTANCE ?: synchronized(this) {
        INSTANCE ?: PlayPay().also { INSTANCE = it }
      }
  }

  /**
   * play 市场一次只能买一个商品
   */
  override suspend fun pay(
    context: Context
  ): Boolean {
    if (skuDetails.isEmpty()) {
      Log.e(TAG, "购买之前需要先获取商品详情")
      return false
    } else {
      val flowParams = BillingFlowParams.newBuilder()
          .setSkuDetails(skuDetails[0])
          .build()

      // 调用play支付
      val result = getClient(context).launchBillingFlow(context as Activity, flowParams)
      val responseCode = result.responseCode
      val debugMessage = result.debugMessage
      Log.d(TAG, "launchBillingFlow: BillingResponse $responseCode $debugMessage")

      when (responseCode) {
        BillingResponseCode.OK -> {
          // 调起play成功进入等待
          try {
            lock.tryLock()
            condition.await(10, SECONDS)
          } catch (e:Exception){
            e.printStackTrace()
          } finally {
            lock.unlock()
          }

        }
        BillingResponseCode.USER_CANCELED -> {
          Log.i(TAG, "onPurchasesUpdated: User canceled the purchase")
          return false
        }
        BillingResponseCode.ITEM_ALREADY_OWNED -> {
          Log.i(TAG, "onPurchasesUpdated: The user already owns this item")
          return false
        }
        BillingResponseCode.DEVELOPER_ERROR -> {
          Log.e(
              TAG, "onPurchasesUpdated: Developer error means that Google Play " +
              "does not recognize the configuration. If you are just getting started, " +
              "make sure you have configured the application correctly in the " +
              "Google Play Console. The SKU product ID must match and the APK you " +
              "are using must be signed with release keys."
          )
          return false
        }
      }
    }
    return false
  }

  override suspend fun queryGoodsDetail(
    context: Context,
    billingParam: BillingParam
  ): List<GoodsEntity> {
    Log.d(TAG, "querySkuDetails")

    val goodsIds = arrayListOf<String>().also { list ->
      billingParam.goodsList.forEach { entity ->
        list.add(entity.id)
      }
    }

    val params = SkuDetailsParams.newBuilder()
        .setType(if (billingParam.skuType == INAPP) SkuType.INAPP else SkuType.SUBS)
        .setSkusList(goodsIds)
        .build()
    // 同步获取数据
    val result = getClient(context).querySkuDetails(params)

    return if (result.billingResult.responseCode == BillingResponseCode.OK && result.skuDetailsList != null) {
      arrayListOf<GoodsEntity>().also { list ->
        result.skuDetailsList!!.forEach { skuDetail ->
          skuDetails.clear()
          skuDetails.addAll(result.skuDetailsList!!)
          // 构造商品详情
          val goods = GoodsEntity()
          goods.id = skuDetail.sku
          goods.name = skuDetail.title
          goods.desc = skuDetail.description
          goods.iconUrl = skuDetail.iconUrl
          goods.price = skuDetail.price
          goods.originalPrice = skuDetail.originalPrice

          list.add(goods)
        }
      }
    } else {
      emptyList()
    }
  }

  override fun destroy() {
    if (billingClient?.isReady!!) {
      billingClient?.endConnection()
    }
  }

  private fun getClient(context: Context): BillingClient {
    if (billingClient == null) {
      billingClient = BillingClient.newBuilder(context).setListener(this).build()
      if (!billingClient!!.isReady) {
        Log.d(TAG, "BillingClient: Start connection...")
        billingClient!!.startConnection(this)
      }
      return billingClient!!
    } else {
      return billingClient!!
    }
  }

  /**
   * 支付回调
   */
  override fun onPurchasesUpdated(
    resule: BillingResult,
    purchase: MutableList<Purchase>?
  ) {
    val responseCode = resule.responseCode
    val debugMessage = resule.debugMessage
    Log.d(TAG, "支付回调: $responseCode $debugMessage")


    try {
      lock.tryLock()
      condition.signalAll()
    } catch (e:Exception){
      e.printStackTrace()
    } finally {
      lock.unlock()
    }
  }

  override fun onBillingServiceDisconnected() {
    Log.d(TAG, "onBillingServiceDisconnected")
  }

  override fun onBillingSetupFinished(billingResult: BillingResult) {
    val responseCode = billingResult.responseCode
    val debugMessage = billingResult.debugMessage
    Log.d(TAG, "onBillingSetupFinished: $responseCode $debugMessage")
  }

}