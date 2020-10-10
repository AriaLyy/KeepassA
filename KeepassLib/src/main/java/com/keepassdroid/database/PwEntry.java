/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database;

import com.keepassdroid.database.iterator.EntrySearchStringIterator;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.UUID;

public abstract class PwEntry implements Cloneable, PwDataInf, Serializable {

  protected static final String PMS_TAN_ENTRY = "<TAN>";

  public static class EntryNameComparator implements Comparator<PwEntry> {

    public int compare(PwEntry object1, PwEntry object2) {
      return object1.getTitle().compareToIgnoreCase(object2.getTitle());
    }
  }

  public PwIconStandard icon = PwIconStandard.FIRST;

  public PwEntry() {

  }

  public static PwEntry getInstance(PwGroup parent) {
    return PwEntry.getInstance(parent, true, true);
  }

  public static PwEntry getInstance(PwGroup parent, boolean initId, boolean initDates) {
    if (parent instanceof PwGroupV3) {
      return new PwEntryV3((PwGroupV3) parent);
    } else if (parent instanceof PwGroupV4) {
      return new PwEntryV4((PwGroupV4) parent);
    } else {
      throw new RuntimeException("Unknown PwGroup instance.");
    }
  }

  @Override
  public Object clone() {
    PwEntry newEntry;
    try {
      newEntry = (PwEntry) super.clone();
    } catch (CloneNotSupportedException e) {
      assert (false);
      throw new RuntimeException("Clone should be supported");
    }

    return newEntry;
  }

  public PwEntry clone(boolean deepStrings) {
    return (PwEntry) clone();
  }

  public void assign(PwEntry source) {
    icon = source.icon;
  }

  public abstract UUID getUUID();

  public abstract void setUUID(UUID u);

  public String getTitle() {
    return getTitle(false, null);
  }

  public String getUsername() {
    return getUsername(false, null);
  }

  public String getPassword() {
    return getPassword(false, null);
  }

  public String getUrl() {
    return getUrl(false, null);
  }

  public String getNotes() {
    return getNotes(false, null);
  }

  public abstract String getTitle(boolean decodeRef, PwDatabase db);

  public abstract String getUsername(boolean decodeRef, PwDatabase db);

  public abstract String getPassword(boolean decodeRef, PwDatabase db);

  public abstract String getUrl(boolean decodeRef, PwDatabase db);

  public abstract String getNotes(boolean decodeRef, PwDatabase db);

  public abstract Date getLastModificationTime();

  public abstract Date getLastAccessTime();

  public abstract Date getExpiryTime();

  public abstract boolean expires();

  public abstract void setTitle(String title, PwDatabase db);

  public abstract void setUsername(String user, PwDatabase db);

  public abstract void setPassword(String pass, PwDatabase db);

  public abstract void setUrl(String url, PwDatabase db);

  public abstract void setNotes(String notes, PwDatabase db);

  public abstract void setCreationTime(Date create);

  public abstract void setLastModificationTime(Date mod);

  public abstract void setLastAccessTime(Date access);

  public abstract void setExpires(boolean exp);

  public abstract void setExpiryTime(Date expires);

  public PwIcon getIcon() {
    return icon;
  }

  public boolean isTan() {
    return getTitle().equals(PMS_TAN_ENTRY) && (getUsername().length() > 0);
  }

  public String getDisplayTitle() {
    if (isTan()) {
      return PMS_TAN_ENTRY + " " + getUsername();
    } else {
      return getTitle();
    }
  }

  public boolean isMetaStream() {
    return false;
  }

  public EntrySearchStringIterator stringIterator() {
    return EntrySearchStringIterator.getInstance(this);
  }

  public void touch(boolean modified, boolean touchParents) {
    Date now = new Date();

    setLastAccessTime(now);

    if (modified) {
      setLastModificationTime(now);
    }

    PwGroup parent = getParent();
    if (touchParents && parent != null) {
      parent.touch(modified, true);
    }
  }

  public void touchLocation() {
  }

  public abstract void setParent(PwGroup parent);

  public boolean isSearchingEnabled() {
    return false;
  }
}