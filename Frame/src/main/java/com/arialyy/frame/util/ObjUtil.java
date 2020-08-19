/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.arialyy.frame.util;

import java.util.Map;
import java.util.Set;

/**
 * Created by lyy on 2016/7/6.
 */
public class ObjUtil {

  /**
   * 打印Map
   */
  public static String getMapString(Map map) {
    Set set = map.keySet();
    if (set.size() < 1) {
      return "[]";
    }
    StringBuilder strBuilder = new StringBuilder();
    Object[] array = set.toArray();
    strBuilder.append("[").append(array[0]).append("=").append(map.get(array[0]));
    for (int i = 1; i < array.length; ++i) {
      strBuilder.append(", ");
      strBuilder.append(array[i]).append("=");
      strBuilder.append(map.get(array[i]));
    }
    strBuilder.append("]");
    return strBuilder.toString();
  }

  /**
   * 通过Value 获取key
   */
  public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
    for (Map.Entry<T, E> entry : map.entrySet()) {
      if (equals(value, entry.getValue())) {
        return entry.getKey();
      }
    }
    return null;
  }

  /**
   * 比较两个对象是否相等
   */
  public static boolean equals(Object a, Object b) {
    return (a == null) ? (b == null) : a.equals(b);
  }
}