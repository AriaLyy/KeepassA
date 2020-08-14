package com.lyy.ibaseapi

import com.lyy.ibaseapi.SkuType.INAPP

/**
 * 支付参数
 */
class BillingParam {

  /**
   * 购买的商品列表
   */
  var goodsList: List<GoodsEntity> = ArrayList()

  /**
   * 购买数量
   */
  var goodsNum: Int = 1

  /**
   * 商品类型
   */
  var skuType: SkuType = INAPP

}