package com.lyy.ibaseapi

import android.content.Context

interface IPlay {

  /**
   * 支付，支付之前需要先查询商品详情
   * @return true 购买成功
   */
  suspend fun pay(
    context: Context
  ): Boolean

  /**
   * 查询商品详情
   */
  suspend fun queryGoodsDetail(
    context: Context,
    billingParam: BillingParam
  ): List<GoodsEntity>

  fun destroy()
}