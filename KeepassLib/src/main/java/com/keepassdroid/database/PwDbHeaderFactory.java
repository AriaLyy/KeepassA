/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database;

public class PwDbHeaderFactory {
  public static PwDbHeader getInstance(PwDatabase db) {
    if (db instanceof PwDatabaseV3) {
      return new PwDbHeaderV3();
    } else if (db instanceof PwDatabaseV4) {
      return new PwDbHeaderV4((PwDatabaseV4) db);
    } else {
      throw new RuntimeException("Not implemented.");
    }
  }
}