/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database;

import androidx.annotation.NonNull;

public class PwIconStandard extends PwIcon implements Cloneable {
  public final int iconId;

  public static PwIconStandard FIRST = new PwIconStandard(1);

  public static final int TRASH_BIN = 43;
  public static final int FOLDER = 48;

  public PwIconStandard(int iconId) {
    this.iconId = iconId;
  }

  @Override
  public boolean isMetaStreamIcon() {
    return iconId == 0;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + iconId;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PwIconStandard)) {
      return false;
    }

    if (this == obj) {
      return true;
    }
    PwIconStandard other = (PwIconStandard) obj;
    return iconId == other.iconId;
  }

  @NonNull @Override public PwIconStandard clone() throws CloneNotSupportedException {
    return (PwIconStandard) super.clone();
  }
}