/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arialyy.frame.util.RegularRule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.net.URL

@RunWith(AndroidJUnit4::class)
class UrlTest {

  @Test
  fun domainTest(){
    val temp = "twitter.com"
    val topDomain = Regex(RegularRule.DOMAIN_TOP, RegexOption.IGNORE_CASE).find(temp)
    println("topDomain = ${topDomain?.value}")
  }

  @Test
  fun testUrl(){
    try {
      val url = URL("http://www.runoob.com/ssss/ssxxx/index.html?language=cn#j2se")
      System.out.println("URL 为：" + url.toString())
      System.out.println("协议为：" + url.getProtocol())
      System.out.println("验证信息：" + url.getAuthority())
      System.out.println("文件名及请求参数：" + url.getFile())
      System.out.println("主机名：" + url.getHost())
      System.out.println("路径：" + url.getPath())
      System.out.println("端口：" + url.getPort())
      System.out.println("默认端口：" + url.getDefaultPort())
      System.out.println("请求参数：" + url.getQuery())
      System.out.println("定位位置：" + url.getRef())
    } catch (e: IOException) {
      e.printStackTrace()
    }
  }

  @Test
  fun testUri(){
    try {
      val url = Uri.parse("http://www.runoob.com/ssss/ssxxx/index.html?language=cn&q=1#j2se")
      println("URL 为：$url")
      println("协议为：" + url.scheme)
      println("验证信息：" + url.authority)
      println("文件名及请求参数：" + url.query)
      println("主机名：" + url.host)
      println("路径：" + url.path)
      println("最后一段路径：" + url.lastPathSegment)
      println("端口：" + url.port)
      println("请求参数：" + url.query)
      println("请求参数key：" + url.queryParameterNames)
      println("请求参数language：" + url.getQueryParameters("language"))
      println("定位位置：" + url.fragment)
    } catch (e: IOException) {
      e.printStackTrace()
    }
  }
}