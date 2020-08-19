/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.arialyy.frame.util;

import android.app.ActivityManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.StatFs;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import androidx.core.content.FileProvider;
import com.arialyy.frame.util.show.L;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 文件操作工具 可以创建和删除文件等
 */
public class FileUtil {
  private static final String KB = "KB";
  private static final String MB = "MB";
  private static final String GB = "GB";

  private static final String TAG = "FileUtil";

  /**
   * 使用系统应用打开文件
   */
  public static void openFile(Context context, File file) {
    Intent intent = new Intent();
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    //设置intent的Action属性
    intent.setAction(Intent.ACTION_VIEW);

    Uri uri = null;
    // 支持Android7.0，Android 7.0以后，用了Content Uri 替换了原本的File Uri
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    } else {
      uri = Uri.fromFile(file);
    }

    //获取文件file的MIME类型
    String type = getMIMEType(file);
    //设置intent的data和Type属性。
    intent.setDataAndType(uri, type);
    //跳转
    try {
      context.startActivity(intent);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 转换 content:// uri
   *
   * @param imageFile imageFile
   */
  public static Uri getImageContentUri(Context context, File imageFile) {
    String filePath = imageFile.getAbsolutePath();
    Cursor cursor = context.getContentResolver().query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        new String[] { MediaStore.Images.Media._ID },
        MediaStore.Images.Media.DATA + "=? ",
        new String[] { filePath }, null);

    if (cursor != null && cursor.moveToFirst()) {
      int id = cursor.getInt(cursor
          .getColumnIndex(MediaStore.MediaColumns._ID));
      Uri baseUri = Uri.parse("content://media/external/images/media");
      return Uri.withAppendedPath(baseUri, "" + id);
    } else {
      if (imageFile.exists()) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATA, filePath);
        return context.getContentResolver().insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
      } else {
        return null;
      }
    }
  }

  /**
   * 根据文件后缀回去MIME类型
   *
   * @param file file
   * @return string
   */
  private static String getMIMEType(File file) {
    String type = "*/*";
    String fName = file.getName();

    //获取后缀名前的分隔符"."在fName中的位置。
    int dotIndex = fName.lastIndexOf(".");
    if (dotIndex < 0) {
      return type;
    }

    /* 获取文件的后缀名*/
    String end = fName.substring(dotIndex, fName.length()).toLowerCase();
    if (TextUtils.isEmpty(end)) {
      return type;
    }

    //在MIME和文件类型的匹配表中找到对应的MIME类型。
    for (String[] strings : MIME_MapTable) {
      if (end.equals(strings[0])) {
        type = strings[1];
        break;
      }
    }
    return type;
  }

  private static final String[][] MIME_MapTable = {
      // {后缀名，MIME类型}
      { ".3gp", "video/3gpp" },
      { ".apk", "application/vnd.android.package-archive" },
      { ".asf", "video/x-ms-asf" },
      { ".avi", "video/x-msvideo" },
      { ".bin", "application/octet-stream" },
      { ".bmp", "image/bmp" },
      { ".c", "text/plain" },
      { ".class", "application/octet-stream" },
      { ".conf", "text/plain" },
      { ".cpp", "text/plain" },
      { ".doc", "application/msword" },
      {
          ".docx",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
      },
      { ".xls", "application/vnd.ms-excel" },
      {
          ".xlsx",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
      },
      { ".exe", "application/octet-stream" },
      { ".gif", "image/gif" },
      { ".gtar", "application/x-gtar" },
      { ".gz", "application/x-gzip" },
      { ".h", "text/plain" },
      { ".htm", "text/html" },
      { ".html", "text/html" },
      { ".jar", "application/java-archive" },
      { ".java", "text/plain" },
      { ".jpeg", "image/jpeg" },
      { ".jpg", "image/jpeg" },
      { ".js", "application/x-javascript" },
      { ".log", "text/plain" },
      { ".m3u", "audio/x-mpegurl" },
      { ".m4a", "audio/mp4a-latm" },
      { ".m4b", "audio/mp4a-latm" },
      { ".m4p", "audio/mp4a-latm" },
      { ".m4u", "video/vnd.mpegurl" },
      { ".m4v", "video/x-m4v" },
      { ".mov", "video/quicktime" },
      { ".mp2", "audio/x-mpeg" },
      { ".mp3", "audio/x-mpeg" },
      { ".mp4", "video/mp4" },
      { ".mpc", "application/vnd.mpohun.certificate" },
      { ".mpe", "video/mpeg" },
      { ".mpeg", "video/mpeg" },
      { ".mpg", "video/mpeg" },
      { ".mpg4", "video/mp4" },
      { ".mpga", "audio/mpeg" },
      { ".msg", "application/vnd.ms-outlook" },
      { ".ogg", "audio/ogg" },
      { ".pdf", "application/pdf" },
      { ".png", "image/png" },
      { ".pps", "application/vnd.ms-powerpoint" },
      { ".ppt", "application/vnd.ms-powerpoint" },
      {
          ".pptx",
          "application/vnd.openxmlformats-officedocument.presentationml.presentation"
      },
      { ".prop", "text/plain" }, { ".rc", "text/plain" },
      { ".rmvb", "audio/x-pn-realaudio" }, { ".rtf", "application/rtf" },
      { ".sh", "text/plain" }, { ".tar", "application/x-tar" },
      { ".tgz", "application/x-compressed" }, { ".txt", "text/plain" },
      { ".wav", "audio/x-wav" }, { ".wma", "audio/x-ms-wma" },
      { ".wmv", "audio/x-ms-wmv" },
      { ".wps", "application/vnd.ms-works" }, { ".xml", "text/plain" },
      { ".z", "application/x-compress" },
      { ".zip", "application/x-zip-compressed" }, { "", "*/*" }
  };

  /**
   * 通过流创建文件
   *
   * @param dest 输出路径
   */
  public static void createFileFormInputStream(InputStream is, String dest) {
    try {
      FileOutputStream fos = new FileOutputStream(dest);
      byte[] buf = new byte[1024];
      int len;
      while ((len = is.read(buf)) > 0) {
        fos.write(buf, 0, len);
      }
      is.close();
      fos.flush();
      fos.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * 创建目录 当目录不存在的时候创建文件，否则返回false
   */
  public static boolean createDir(String path) {
    File file = new File(path);
    if (!file.exists()) {
      if (!file.mkdirs()) {
        Log.d(TAG, "创建失败，请检查路径和是否配置文件权限！");
        return false;
      }
      return true;
    }
    return false;
  }

  /**
   * 创建文件 当文件不存在的时候就创建一个文件。 如果文件存在，先删除原文件，然后重新创建一个新文件
   *
   * @return {@code true} 创建成功、{@code false} 创建失败
   */
  public static boolean createFile(String path) {
    if (TextUtils.isEmpty(path)) {
      Log.e(TAG, "文件路径不能为null");
      return false;
    }
    return createFile(new File(path));
  }

  /**
   * 创建文件 当文件不存在的时候就创建一个文件。 如果文件存在，先删除原文件，然后重新创建一个新文件
   *
   * @return {@code true} 创建成功、{@code false} 创建失败
   */
  public static boolean createFile(File file) {
    if (file.getParentFile() == null || !file.getParentFile().exists()) {
      Log.d(TAG, "目标文件所在路径不存在，准备创建……");
      if (!createDir(file.getParent())) {
        Log.d(TAG, "创建目录文件所在的目录失败！文件路径【" + file.getPath() + "】");
      }
    }
    // 文件存在，删除文件
    deleteFile(file);
    try {
      if (file.createNewFile()) {
        //Log.d(TAG, "创建文件成功:" + file.getAbsolutePath());
        return true;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return false;
  }

  /**
   * 创建文件名，如果url链接有后缀名，则使用url中的后缀名
   *
   * @return url 的 hashKey
   */
  public static String createFileName(String url) {
    int end = url.indexOf("?");
    String tempUrl, fileName = "";
    if (end > 0) {
      tempUrl = url.substring(0, end);
      int tempEnd = tempUrl.lastIndexOf("/");
      if (tempEnd > 0) {
        fileName = tempUrl.substring(tempEnd + 1);
      }
    } else {
      int tempEnd = url.lastIndexOf("/");
      if (tempEnd > 0) {
        fileName = url.substring(tempEnd + 1);
      }
    }
    if (TextUtils.isEmpty(fileName)) {
      fileName = StringUtil.keyToHashKey(url);
    }
    return fileName;
  }

  /**
   * 删除文件
   *
   * @param path 文件路径
   * @return {@code true}删除成功、{@code false}删除失败
   */
  public static boolean deleteFile(String path) {
    if (TextUtils.isEmpty(path)) {
      Log.e(TAG, "删除文件失败，路径为空");
      return false;
    }
    return deleteFile(new File(path));
  }

  /**
   * 删除文件
   *
   * @param file 文件路径
   * @return {@code true}删除成功、{@code false}删除失败
   */
  public static boolean deleteFile(File file) {
    if (file.exists()) {
      final File to = new File(file.getAbsolutePath() + System.currentTimeMillis());
      if (file.renameTo(to)) {
        return to.delete();
      } else {
        return file.delete();
      }
    }
    return false;
  }

  /**
   * 删除文件夹
   */
  public static boolean deleteDir(File dirFile) {
    // 如果dir对应的文件不存在，则退出
    if (!dirFile.exists()) {
      return false;
    }

    if (dirFile.isFile()) {
      return dirFile.delete();
    } else {

      for (File file : dirFile.listFiles()) {
        deleteDir(file);
      }
    }

    return dirFile.delete();
  }

  /**
   * 将对象写入到文件
   *
   * @param filePath 文件保存路径
   * @return true 写入成功，false 写入失败
   */
  public static boolean writeObjToFile(String filePath, Parcelable parceable) {
    Parcel parcel = Parcel.obtain();
    parcel.setDataPosition(0);
    parceable.writeToParcel(parcel, 0);
    byte[] bytes = parcel.marshall();

    parcel.recycle();
    try {
      FileOutputStream fos = new FileOutputStream(filePath);
      fos.write(bytes, 0, bytes.length);
      fos.flush();
      fos.close();
      return true;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }

  /**
   * 从文件中读取对象
   * @param filePath 文件保存路径
   * @return null，读取失败， Parcel 对象，使用TestParcel.CREATOR(parcel)进行序列化
   */
  public static Parcel readObjFromParcel(String filePath) {
    Parcel parcel = Parcel.obtain();

    try {
      FileInputStream fis = new FileInputStream(filePath);
      byte[] bytes = new byte[fis.available()];
      parcel.unmarshall(bytes, 0, bytes.length);
      parcel.setDataPosition(0);
      return parcel;
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * 将对象写入文件
   *
   * @param filePath 文件路径
   * @param data data数据必须实现{@link Serializable}接口
   */
  public static void writeObjToFile(String filePath, Serializable data) {

    FileOutputStream ops = null;
    try {
      if (!createFile(filePath)) {
        return;
      }
      ops = new FileOutputStream(filePath);
      ObjectOutputStream oops = new ObjectOutputStream(ops);
      oops.writeObject(data);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (ops != null) {
        try {
          ops.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * 从文件中读取对象
   *
   * @param filePath 文件路径
   * @return 如果读取成功，返回相应的Obj对象，读取失败，返回null
   */
  public static Serializable readObjFromFile(String filePath) {
    if (TextUtils.isEmpty(filePath)) {
      Log.e(TAG, "文件路径为空");
      return null;
    }
    File file = new File(filePath);
    if (!file.exists()) {
      Log.e(TAG, String.format("文件【%s】不存在", filePath));
      return null;
    }
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(filePath);
      ObjectInputStream oois = new ObjectInputStream(fis);
      return (Serializable) oois.readObject();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return null;
  }

  /**
   * 合并文件
   *
   * @param targetPath 目标文件
   * @param subPaths 碎片文件路径
   * @return {@code true} 合并成功，{@code false}合并失败
   */
  public static boolean mergeFile(String targetPath, List<String> subPaths) {
    File file = new File(targetPath);
    FileOutputStream fos = null;
    FileChannel foc = null;
    long startTime = System.currentTimeMillis();
    try {
      if (file.exists() && file.isDirectory()) {
        Log.w(TAG, String.format("路径【%s】是文件夹，将删除该文件夹", targetPath));
        FileUtil.deleteDir(file);
      }
      if (!file.exists()) {
        FileUtil.createFile(file);
      }

      fos = new FileOutputStream(targetPath);
      foc = fos.getChannel();
      List<FileInputStream> streams = new LinkedList<>();
      long fileLen = 0;
      for (String subPath : subPaths) {
        File f = new File(subPath);
        if (!f.exists()) {
          Log.d(TAG, String.format("合并文件失败，文件【%s】不存在", subPath));
          for (FileInputStream fis : streams) {
            fis.close();
          }
          streams.clear();

          return false;
        }
        FileInputStream fis = new FileInputStream(subPath);
        FileChannel fic = fis.getChannel();
        foc.transferFrom(fic, fileLen, f.length());
        fileLen += f.length();
        fis.close();
      }
      Log.d(TAG, String.format("合并文件耗时：%sms", (System.currentTimeMillis() - startTime)));
      return true;
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (foc != null) {
          foc.close();
        }
        if (fos != null) {
          fos.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  /**
   * 分割文件
   *
   * @param filePath 被分割的文件路径
   * @param num 分割的块数
   */
  public static void splitFile(String filePath, int num) {
    try {
      final File file = new File(filePath);
      long size = file.length();
      FileInputStream fis = new FileInputStream(file);
      FileChannel fic = fis.getChannel();
      long j = 0;
      long block = size / num;
      for (int i = 0; i < num; i++) {
        if (i == num - 1) {
          block = size - block * (num - 1);
        }
        String subPath = file.getPath() + "." + i + ".part";
        Log.d(TAG, String.format("block = %s", block));
        File subFile = new File(subPath);
        if (!subFile.exists()) {
          createFile(subFile);
        }
        FileOutputStream fos = new FileOutputStream(subFile);
        FileChannel sfoc = fos.getChannel();
        ByteBuffer bf = ByteBuffer.allocate(8196);
        int len;
        //fis.skip(block * i);
        while ((len = fic.read(bf)) != -1) {
          bf.flip();
          sfoc.write(bf);
          bf.compact();
          j += len;
          if (j >= block * (i + 1)) {
            break;
          }
        }
        Log.d(TAG, String.format("len = %s", subFile.length()));
        fos.close();
        sfoc.close();
      }
      fis.close();
      fic.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * 检查内存空间是否充足
   *
   * @param path 文件路径
   * @param fileSize 下载的文件的大小
   * @return true 空间足够
   */
  public static boolean checkMemorySpace(String path, long fileSize) {
    File temp = new File(path);
    if (!temp.exists()) {
      if (!temp.getParentFile().exists()) {
        FileUtil.createDir(temp.getParentFile().getPath());
      }
      path = temp.getParentFile().getPath();
    }

    StatFs stat = new StatFs(path);
    long blockSize = stat.getBlockSize();
    long availableBlocks = stat.getAvailableBlocks();
    return fileSize <= availableBlocks * blockSize;
  }

  /**
   * 读取下载配置文件
   */
  public static Properties loadConfig(File file) {
    Properties properties = new Properties();
    FileInputStream fis = null;
    if (!file.exists()) {
      createFile(file.getPath());
    }
    try {
      fis = new FileInputStream(file);
      properties.load(fis);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (fis != null) {
          fis.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return properties;
  }

  /**
   * 保存配置文件
   */
  public static void saveConfig(File file, Properties properties) {
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(file, false);
      properties.store(fos, null);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (fos != null) {
          fos.flush();
          fos.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * sdcard 可用大小
   *
   * @param sdcardPath sdcard 根路径
   * @return 单位为：byte
   */
  private static long getAvailableExternalMemorySize(String sdcardPath) {
    StatFs stat = new StatFs(sdcardPath);
    long blockSize = stat.getBlockSize();
    long availableBlocks = stat.getAvailableBlocks();
    return availableBlocks * blockSize;
  }

  /**
   * sdcard 总大小
   *
   * @param sdcardPath sdcard 根路径
   * @return 单位为：byte
   */
  private static long getTotalExternalMemorySize(String sdcardPath) {
    StatFs stat = new StatFs(sdcardPath);
    long blockSize = stat.getBlockSize();
    long totalBlocks = stat.getBlockCount();
    return totalBlocks * blockSize;
  }

  private static boolean canWrite(String dirPath) {
    File dir = new File(dirPath);
    if (dir.canWrite()) {
      return true;
    }
    boolean canWrite;
    File testWriteFile = null;
    try {
      testWriteFile = new File(dirPath, "tw.txt");
      if (testWriteFile.exists()) {
        testWriteFile.delete();
      }
      testWriteFile.createNewFile();
      FileWriter writer = new FileWriter(testWriteFile);
      writer.write(1);
      writer.close();
      canWrite = true;
    } catch (Exception e) {
      e.printStackTrace();
      canWrite = false;
    } finally {
      try {
        if (testWriteFile != null && testWriteFile.exists()) {
          testWriteFile.delete();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return canWrite;
  }

  public static long getTotalMemory() {
    String file_path = "/proc/meminfo";// 系统内存信息文件
    String ram_info;
    String[] arrayOfRam;
    long initial_memory = 0L;
    try {
      FileReader fr = new FileReader(file_path);
      BufferedReader localBufferedReader = new BufferedReader(fr, 8192);
      // 读取meminfo第一行，系统总内存大小
      ram_info = localBufferedReader.readLine();
      arrayOfRam = ram_info.split("\\s+");// 实现多个空格切割的效果
      initial_memory =
          Integer.valueOf(arrayOfRam[1]) * 1024;// 获得系统总内存，单位是KB，乘以1024转换为Byte
      localBufferedReader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return initial_memory;
  }

  public static long getAvailMemory(Context context) {
    ActivityManager activityManager =
        (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
    activityManager.getMemoryInfo(memoryInfo);
    return memoryInfo.availMem;
  }

  //android获取一个用于打开HTML文件的intent
  public static Intent getHtmlFileIntent(String Path) {
    File file = new File(Path);
    Uri uri = Uri.parse(file.toString())
        .buildUpon()
        .encodedAuthority("com.android.htmlfileprovider")
        .scheme("content")
        .encodedPath(file.toString())
        .build();
    Intent intent = new Intent("android.intent.action.VIEW");
    intent.setDataAndType(uri, "text/html");
    return intent;
  }

  /**
   * 获取文件夹大小
   */
  public static long getDirSize(String filePath) {
    long size = 0;
    File f = new File(filePath);
    if (f.isDirectory()) {
      File[] files = f.listFiles();
      for (File file : files) {
        if (file.isDirectory()) {
          size += getDirSize(file.getPath());
          continue;
        }
        size += file.length();
      }
    } else {
      size += f.length();
    }
    return size;
  }

  /**
   * 格式化文件大小
   *
   * @param size file.length() 获取文件大小
   */
  public static String formatFileSize(double size) {
    double kiloByte = size / 1024;
    if (kiloByte < 1) {
      return size + "B";
    }

    double megaByte = kiloByte / 1024;
    if (megaByte < 1) {
      BigDecimal result1 = new BigDecimal(Double.toString(kiloByte));
      return result1.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "KB";
    }

    double gigaByte = megaByte / 1024;
    if (gigaByte < 1) {
      BigDecimal result2 = new BigDecimal(Double.toString(megaByte));
      return result2.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "MB";
    }

    double teraBytes = gigaByte / 1024;
    if (teraBytes < 1) {
      BigDecimal result3 = new BigDecimal(Double.toString(gigaByte));
      return result3.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "GB";
    }
    BigDecimal result4 = new BigDecimal(teraBytes);
    return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "TB";
  }

  /**
   * 获取文件后缀名
   */
  public static String getFileExtensionName(String fileName) {
    if (TextUtils.isEmpty(fileName)) {
      return "";
    }
    int endP = fileName.lastIndexOf(".");
    return endP > -1 ? fileName.substring(endP + 1, fileName.length()) : "";
  }

  /**
   * 校验文件MD5码
   */
  public static boolean checkMD5(String md5, File updateFile) {
    if (TextUtils.isEmpty(md5) || updateFile == null) {
      L.e(TAG, "MD5 string empty or updateFile null");
      return false;
    }

    String calculatedDigest = getFileMD5(updateFile);
    if (calculatedDigest == null) {
      L.e(TAG, "calculatedDigest null");
      return false;
    }
    return calculatedDigest.equalsIgnoreCase(md5);
  }

  /**
   * 获取文件MD5码
   */
  public static String getFileMD5(File updateFile) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      L.e(TAG, "Exception while getting digest", e);
      return null;
    }

    InputStream is;
    try {
      is = new FileInputStream(updateFile);
    } catch (FileNotFoundException e) {
      L.e(TAG, "Exception while getting FileInputStream", e);
      return null;
    }

    byte[] buffer = new byte[8192];
    int read;
    try {
      while ((read = is.read(buffer)) > 0) {
        digest.update(buffer, 0, read);
      }
      byte[] md5sum = digest.digest();
      BigInteger bigInt = new BigInteger(1, md5sum);
      String output = bigInt.toString(16);
      // Fill to 32 chars
      output = String.format("%32s", output).replace(' ', '0');
      return output;
    } catch (IOException e) {
      throw new RuntimeException("Unable to process file for MD5", e);
    } finally {
      try {
        is.close();
      } catch (IOException e) {
        L.e(TAG, "Exception on closing MD5 input stream", e);
      }
    }
  }

  /**
   * 解压缩功能.
   * 将ZIP_FILENAME文件解压到ZIP_DIR目录下.
   *
   * @param zipFile 压缩文件
   * @param folderPath 解压目录
   */
  public static int unZipFile(File zipFile, String folderPath) {
    ZipFile zfile = null;
    try {
      zfile = new ZipFile(zipFile);
      Enumeration zList = zfile.entries();
      ZipEntry ze = null;
      byte[] buf = new byte[1024];
      while (zList.hasMoreElements()) {
        ze = (ZipEntry) zList.nextElement();
        if (ze.isDirectory()) {
          //                    L.d(TAG, "ze.getName() = " + ze.getName());
          String dirstr = folderPath + ze.getName();
          //dirstr.trim();
          dirstr = new String(dirstr.getBytes("8859_1"), "GB2312");
          //                    L.d(TAG, "str = " + dirstr);
          File f = new File(dirstr);
          f.mkdir();
          continue;
        }
        //                L.d(TAG, "ze.getName() = " + ze.getName());
        OutputStream os = new BufferedOutputStream(
            new FileOutputStream(getRealFileName(folderPath, ze.getName())));
        InputStream is = new BufferedInputStream(zfile.getInputStream(ze));
        int readLen = 0;
        while ((readLen = is.read(buf)) != -1) {
          os.write(buf, 0, readLen);
        }
        is.close();
        os.close();
      }
      zfile.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return 0;
  }

  /**
   * 给定根目录，返回一个相对路径所对应的实际文件名.
   *
   * @param baseDir 指定根目录
   * @param absFileName 相对路径名，来自于ZipEntry中的name
   * @return java.io.File 实际的文件
   */
  private static File getRealFileName(String baseDir, String absFileName) {
    String[] dirs = absFileName.split("/");
    File ret = new File(baseDir);
    String substr = null;
    if (dirs.length > 1) {
      for (int i = 0; i < dirs.length - 1; i++) {
        substr = dirs[i];
        try {
          //substr.trim();
          substr = new String(substr.getBytes("8859_1"), "GB2312");
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
        ret = new File(ret, substr);
      }
      //            L.d("upZipFile", "1ret = " + ret);
      if (!ret.exists()) ret.mkdirs();
      substr = dirs[dirs.length - 1];
      try {
        //substr.trim();
        substr = new String(substr.getBytes("8859_1"), "GB2312");
        //                L.d("upZipFile", "substr = " + substr);
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
      ret = new File(ret, substr);
      //            L.d("upZipFile", "2ret = " + ret);
      return ret;
    }

    return ret;
  }

  /**
   * 拷贝文件
   */
  public static boolean copy(String fromPath, String toPath) {
    File file = new File(fromPath);
    if (!file.exists()) {
      return false;
    }
    createFile(toPath);
    return copyFile(fromPath, toPath);
  }

  /**
   * 拷贝文件
   */
  private static boolean copyFile(String fromFile, String toFile) {
    InputStream fosfrom = null;
    OutputStream fosto = null;
    try {
      fosfrom = new FileInputStream(fromFile);
      fosto = new FileOutputStream(toFile);
      byte bt[] = new byte[1024];
      int c;
      while ((c = fosfrom.read(bt)) > 0) {
        fosto.write(bt, 0, c);
      }
      return true;
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    } finally {
      try {
        if (fosfrom != null) {
          fosfrom.close();
        }
        if (fosto != null) {
          fosto.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * 递归返回文件或者目录的大小（单位:KB）
   * 不建议使用这个方法，有点坑
   * 可以使用下面的方法：http://blog.csdn.net/loongggdroid/article/details/12304695
   */
  private static float getSize(String path, Float size) {
    File file = new File(path);
    if (file.exists()) {
      if (file.isDirectory()) {
        String[] children = file.list();
        for (String child : children) {
          float tmpSize =
              getSize(file.getPath() + File.separator + child, size) / 1000;
          size += tmpSize;
        }
      } else if (file.isFile()) {
        size += file.length();
      }
    }
    return size;
  }

  /**
   * 获取apk文件的icon
   *
   * @param path apk文件路径
   */
  public static Drawable getApkIcon(Context context, String path) {
    PackageManager pm = context.getPackageManager();
    PackageInfo info = pm.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
    if (info != null) {
      ApplicationInfo appInfo = info.applicationInfo;
      //android有bug，需要下面这两句话来修复才能获取apk图片
      appInfo.sourceDir = path;
      appInfo.publicSourceDir = path;
      //			    String packageName = appInfo.packageName;  //得到安装包名称
      //	            String version=info.versionName;       //得到版本信息
      return pm.getApplicationIcon(appInfo);
    }
    return null;
  }
}