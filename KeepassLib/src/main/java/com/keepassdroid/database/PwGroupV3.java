/*
 * Copyright 2009 Brian Pellin.

This file was derived from

Copyright 2007 Naomaru Itoi <nao@phoneid.org>

This file was derived from 

Java clone of KeePass - A KeePass file viewer for Java
Copyright 2006 Bill Zwicky <billzwicky@users.sourceforge.net>

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
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author Brian Pellin <bpellin@gmail.com>
 * @author Naomaru Itoi <nao@phoneid.org>
 * @author Bill Zwicky <wrzwicky@pobox.com>
 * @author Dominik Reichl <dominik.reichl@t-online.de>
 */
public class PwGroupV3 extends PwGroup {
  public PwGroupV3() {
  }

  public String toString() {
    return name;
  }

  public static final Date NEVER_EXPIRE = PwEntryV3.NEVER_EXPIRE;

  /** Size of byte buffer needed to hold this struct. */
  public static final int BUF_SIZE = 124;

  // for tree traversing
  public PwGroupV3 parent = null;

  public int groupId;

  public PwDate tCreation;
  public PwDate tLastMod;
  public PwDate tLastAccess;
  public PwDate tExpire;

  public int level; // short

  /** Used by KeePass internally, don't use */
  public int flags;

  @NonNull @Override public PwGroupV3 clone() throws CloneNotSupportedException {
    return (PwGroupV3) super.clone();
  }

  @Override public boolean equals(Object obj) {
    if (!(obj instanceof PwGroupV3)) {
      return false;
    }
    PwGroupV3 otherGroup = (PwGroupV3) obj;
    return name.equals(otherGroup.name)
        && icon.equals(otherGroup.icon)
        && tExpire.equals(otherGroup.tExpire);
  }

  @Override public void assign(PwGroup source) {
    if (!(source instanceof PwGroupV3)) {
      throw new RuntimeException("DB version mix");
    }
    super.assign(source);
    PwGroupV3 v3Group = (PwGroupV3) source;

    parent = v3Group.parent;
    groupId = v3Group.groupId;
    tCreation = v3Group.tCreation;
    tLastMod = v3Group.tLastMod;
    tLastAccess = v3Group.tLastAccess;
    tExpire = v3Group.tExpire;
    level = v3Group.level;
    flags = v3Group.flags;
  }

  public void setGroups(List<PwGroup> groups) {
    childGroups = groups;
  }

  @Override
  public PwGroup getParent() {
    return parent;
  }

  @Override
  public PwGroupId getId() {
    return new PwGroupIdV3(groupId);
  }

  @Override
  public void setId(PwGroupId id) {
    PwGroupIdV3 id3 = (PwGroupIdV3) id;
    groupId = id3.getId();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Date getLastMod() {
    return tLastMod.getJDate();
  }

  @Override
  public void setParent(PwGroup prt) {
    parent = (PwGroupV3) prt;
    level = parent.level + 1;
  }

  @Override
  public void initNewGroup(String nm, PwGroupId newId) {
    super.initNewGroup(nm, newId);

    Date now = Calendar.getInstance().getTime();
    tCreation = new PwDate(now);
    tLastAccess = new PwDate(now);
    tLastMod = new PwDate(now);
    tExpire = new PwDate(PwGroupV3.NEVER_EXPIRE);
  }

  public void populateBlankFields(PwDatabaseV3 db) {
    if (icon == null) {
      icon = db.iconFactory.getIcon(1);
    }

    if (name == null) {
      name = "";
    }

    if (tCreation == null) {
      tCreation = PwEntryV3.DEFAULT_PWDATE;
    }

    if (tLastMod == null) {
      tLastMod = PwEntryV3.DEFAULT_PWDATE;
    }

    if (tLastAccess == null) {
      tLastAccess = PwEntryV3.DEFAULT_PWDATE;
    }

    if (tExpire == null) {
      tExpire = PwEntryV3.DEFAULT_PWDATE;
    }
  }

  @Override
  public void setLastAccessTime(Date date) {
    tLastAccess = new PwDate(date);
  }

  @Override
  public void setLastModificationTime(Date date) {
    tLastMod = new PwDate(date);
  }

  @Override public void touchLocation() {
    super.touchLocation();
    // v3 数据库没有父群组修改时间的字段
  }
}
