/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.keepassdroid.database;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PwCustomData extends HashMap<String, String> {
  public Map<String, Date> lastMod = new HashMap<String, Date>();

  public String put(String key, String value, Date last) {
    lastMod.put(key, last);

    return put(key, value);
  }

  public Date getLastMod(String key) {
    return lastMod.get(key);
  }
}