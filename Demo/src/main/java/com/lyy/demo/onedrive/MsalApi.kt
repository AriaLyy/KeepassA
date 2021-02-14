/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.demo.onedrive

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/2/6
 **/
interface MsalApi {

  /**
   *  更新现有文件
   */
  @PUT("users/{user-id}/drive/items/{item-id}/content")
  suspend fun updateFile(
    @Header(MSAL.TOKEN_KEY) authorization: String,
    @Path("user-id") userId: String,
    @Path("item-id") parentId: String
  ): MsalResponse<MsalSourceItem>

  /**
   * 上传新文件
   */
  @PUT("users/{user-id}/drive/items/{parent-id}:/{filename}:/content")
  suspend fun uploadNewFile(
    @Header(MSAL.TOKEN_KEY) authorization: String,
    @Path("userId") userId: String,
    @Path("parent-id") parentId: String,
    @Path("filename") filePath: String
  ): MsalResponse<MsalSourceItem>

  /**
   * 获取驱动器列表，也就是onedrive 空间信息
   */
  @GET("users/{userId}/drives")
  suspend fun getDriveList(
    @Header(MSAL.TOKEN_KEY) authorization: String,
    @Path("userId") userId: String
  ): MsalResponse<List<DriveItem>>

  /**
   * 获取应用的app文件夹列表
   */
  @GET("users/{userId}/drive/special/{special-folder-name}/children")
  suspend fun getAppFolder(
    @Header(MSAL.TOKEN_KEY) authorization: String,
    @Path("userId") userId: String,
    @Path("special-folder-name") folderName: String
  ): MsalResponse<List<MsalSourceItem>>

}