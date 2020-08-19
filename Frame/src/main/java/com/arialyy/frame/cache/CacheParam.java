/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.arialyy.frame.cache;

/**
 * Created by Lyy on 2015/4/9.
 * 缓存参数
 */
public interface CacheParam {

  /**
   * 磁盘缓存
   */
  public static final int DISK_CACHE = 0;
  /**
   * 默认缓存目录文件夹名
   */
  public static final String DEFAULT_DIR = "defaultDir";
  /**
   * 内存缓存
   */
  public static final int MEMORY_CACHE_SIZE = 1;
  /**
   * 小容量磁盘缓存
   */
  public static final long SMALL_DISK_CACHE_CAPACITY = 4 * 1024 * 1024;
  /**
   * 中型容量磁盘缓存
   */
  public static final long NORMAL_DISK_CACHE_CAPACITY = 10 * 1024 * 1024;
  /**
   * 大容量磁盘缓存
   */
  public static final long LARGER_DISKCACHE_CAPACITY = 20 * 1024 * 1024;
  /**
   * 缓存index
   */
  public static final int DISK_CACHE_INDEX = 0;
}