/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database;

import java.util.Date;
import java.util.UUID;

public class PwIconCustom extends PwIcon {
  public static final PwIconCustom ZERO = new PwIconCustom(PwDatabaseV4.UUID_ZERO, new byte[0]);

  public final UUID uuid;
  public byte[] imageData;
  public Date lastMod = null;
  public String name = "";

  public PwIconCustom(UUID u, byte[] data) {
    uuid = u;
    imageData = data;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PwIconCustom)) {
      return false;
    }

    if (this == obj) {
      return true;
    }
    PwIconCustom other = (PwIconCustom) obj;
    if (uuid == null) {
      return other.uuid == null;
    }
    return uuid.equals(other.uuid);
  }
}