/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.utils;

import android.content.Context;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import androidx.documentfile.provider.DocumentFile;
import com.keepassdroid.compat.StorageAF;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by bpellin on 3/5/16.
 */
public class UriUtil {
  private static String TAG = "UriUtil";

  public static Uri parseDefaultFile(String text) {
    if (EmptyUtils.isNullOrEmpty(text)) {
      return null;
    }

    Uri uri = Uri.parse(text);
    if (EmptyUtils.isNullOrEmpty(uri.getScheme())) {
      uri = uri.buildUpon().scheme("file").authority("").build();
    }

    return uri;
  }

  public static Uri parseDefaultFile(Uri uri) {
    if (EmptyUtils.isNullOrEmpty(uri.getScheme())) {
      uri = uri.buildUpon().scheme("file").authority("").build();
    }

    return uri;
  }

  public static boolean equalsDefaultfile(Uri left, String right) {
    if (left == null || right == null) {
      return false;
    }

    left = parseDefaultFile(left);
    Uri uriRight = parseDefaultFile(right);

    return left.equals(uriRight);
  }

  public static InputStream getUriInputStream(Context ctx, Uri uri) throws FileNotFoundException {
    if (uri == null || TextUtils.isEmpty(uri.toString())) {
      return null;
    }
    Log.d(TAG, "uri = " + uri);
    String scheme = uri.getScheme();
    if (EmptyUtils.isNullOrEmpty(scheme) || scheme.equals("file")) {
      return new FileInputStream(uri.getPath());
    } else if (scheme.equals("content")) {
      return ctx.getContentResolver().openInputStream(uri);
    } else {
      return null;
    }
  }

  /**
   * 从uri中获取文件名
   *
   * @return 如果权限不足或获取文件名失败，返回null
   */
  public static String getFileNameFromUri(Context context, Uri uri) {
    if (context == null){
      Log.e(TAG, "content 为空");
      return null;
    }
    if ("content".equalsIgnoreCase(uri.getScheme())) {
      DocumentFile df = DocumentFile.fromSingleUri(context, uri);
      if (df != null) {
        return df.getName();
      }

      if (!checkPermissions(context, uri)) {
        Log.e(TAG, "uri没有授权：" + uri);
        return null;
      }

      Cursor cursor =
          context.getContentResolver().query(uri, null, null, null, null, null);
      if (cursor != null && cursor.moveToFirst()) {
        String displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

        cursor.close();
        return displayName;
      }
    } else if ("file".equalsIgnoreCase(uri.getScheme())) {
      return new File(uri.getPath()).getName();
    }
    return null;
  }

  /**
   * 检查uri 权限
   *
   * @return false 没有权限
   */
  public static boolean checkPermissions(Context context, Uri uri) {
    List<UriPermission> permissions = context.getContentResolver().getPersistedUriPermissions();
    if (permissions.isEmpty()) {
      return false;
    }
    for (UriPermission up : permissions) {
      if (up.getUri().equals(uri) && up.isWritePermission() && up.isReadPermission()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Many android apps respond with non-writeable content URIs that correspond to files.
   * This will attempt to translate the content URIs to file URIs when possible/appropriate
   */
  public static Uri translate(Context ctx, Uri uri) {
    // StorageAF provides nice URIs
    if (StorageAF.useStorageFramework(ctx) || hasWritableContentUri(uri)) {
      return uri;
    }

    String scheme = uri.getScheme();
    if (EmptyUtils.isNullOrEmpty(scheme)) {
      return uri;
    }

    String filepath = null;

    try {
      // Use content resolver to try and find the file
      if (scheme.equalsIgnoreCase("content")) {
        Cursor cursor = ctx.getContentResolver()
            .query(uri, new String[] { android.provider.MediaStore.Images.ImageColumns.DATA }, null,
                null, null);
        cursor.moveToFirst();

        if (cursor != null) {
          filepath = cursor.getString(0);
          cursor.close();

          if (!isValidFilePath(filepath)) {
            filepath = null;
          }
        }
      }

      // Try using the URI path as a straight file
      if (EmptyUtils.isNullOrEmpty(filepath)) {
        filepath = uri.getEncodedPath();
        if (!isValidFilePath(filepath)) {
          filepath = null;
        }
      }
    }
    // Fall back to URI if this fails.
    catch (Exception e) {
      filepath = null;
    }

    // Update the file to a file URI
    if (!EmptyUtils.isNullOrEmpty(filepath)) {
      Uri.Builder b = new Uri.Builder();
      uri = b.scheme("file").authority("").path(filepath).build();
    }

    return uri;
  }

  private static boolean isValidFilePath(String filepath) {
    if (EmptyUtils.isNullOrEmpty(filepath)) {
      return false;
    }

    File file = new File(filepath);
    return file.exists() && file.canRead();
  }

  /**
   * Whitelist for known content providers that support writing
   */
  private static boolean hasWritableContentUri(Uri uri) {
    String scheme = uri.getScheme();

    if (EmptyUtils.isNullOrEmpty(scheme)) {
      return false;
    }

    if (!scheme.equalsIgnoreCase("content")) {
      return false;
    }

    switch (uri.getAuthority()) {
      case "com.google.android.apps.docs.storage":
        return true;
    }

    return false;
  }
}