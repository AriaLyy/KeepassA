package com.lyy.keepassa.view.dialog.webdav

/**
 * @Author laoyuyu
 * @Description
 * @Date 11:25 上午 2022/7/21
 **/
internal interface IWebDavLoginAdapter {

  fun updateState()

  fun startLogin(userName: String, password: String)
}