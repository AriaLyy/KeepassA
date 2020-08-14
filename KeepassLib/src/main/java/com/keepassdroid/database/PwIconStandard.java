/*
 * Copyright 2010-2015 Brian Pellin.
 *
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
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
