/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.arialyy.frame.base.net

import com.arialyy.frame.config.CommonConstant.DEBUG
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit.Builder
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/2/6
 **/
class NetManager1 {

  private lateinit var okHttpClient: OkHttpClient
  private val TIME_OUT = (8 * 1000).toLong()
  private lateinit var baseUrl: String

  fun builderManager(
    baseUrl: String,
    interceptor: List<Interceptor>
  ):NetManager1 {
    this.baseUrl = baseUrl
    okHttpClient = provideOkHttpClient(interceptor)
    return this
  }

  fun getClient() = okHttpClient

  /**
   * 创建OKHTTP
   */
  private fun provideOkHttpClient(interceptor: List<Interceptor>): OkHttpClient {
    val builder = OkHttpClient.Builder()
    if (DEBUG) {
      //HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
      //logging.setLevel(HttpLoggingInterceptor.Level.BODY);
      //builder.addInterceptor(logging);
      builder.addInterceptor(OkHttpLogger())
    }

    interceptor.forEach {
      builder.addInterceptor(it)
    }

    builder
        .connectTimeout(TIME_OUT, MILLISECONDS)
        .readTimeout(TIME_OUT, MILLISECONDS)

    return builder.build()
  }

  fun <SERVICE> request(service: Class<SERVICE>): SERVICE {
    val builder = Builder()
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl(baseUrl)
    return builder.build()
        .create(service)
  }
}