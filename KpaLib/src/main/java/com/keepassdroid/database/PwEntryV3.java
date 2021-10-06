/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database;

// PhoneID

import com.keepassdroid.utils.Types;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

/**
 * Structure containing information about one entry.
 *
 * <PRE>
 * One entry: [FIELDTYPE(FT)][FIELDSIZE(FS)][FIELDDATA(FD)]
 * [FT+FS+(FD)][FT+FS+(FD)][FT+FS+(FD)][FT+FS+(FD)][FT+FS+(FD)]...
 *
 * [ 2 bytes] FIELDTYPE
 * [ 4 bytes] FIELDSIZE, size of FIELDDATA in bytes
 * [ n bytes] FIELDDATA, n = FIELDSIZE
 *
 * Notes:
 * - Strings are stored in UTF-8 encoded form and are null-terminated.
 * - FIELDTYPE can be one of the FT_ constants.
 * </PRE>
 *
 * @author Naomaru Itoi nao@phoneid.org
 * @author Bill Zwicky wrzwicky@pobox.com
 * @author Dominik Reichl dominik.reichl@t-online.de
 */
public class PwEntryV3 extends PwEntry {

  public static final Date NEVER_EXPIRE = getNeverExpire();
  public static final Date NEVER_EXPIRE_BUG = getNeverExpireBug();
  public static final Date DEFAULT_DATE = getDefaultDate();
  public static final PwDate PW_NEVER_EXPIRE = new PwDate(NEVER_EXPIRE);
  public static final PwDate PW_NEVER_EXPIRE_BUG = new PwDate(NEVER_EXPIRE_BUG);
  public static final PwDate DEFAULT_PWDATE = new PwDate(DEFAULT_DATE);

  /** Size of byte buffer needed to hold this struct. */
  public static final String PMS_ID_BINDESC = "bin-stream";
  public static final String PMS_ID_TITLE = "Meta-Info";
  public static final String PMS_ID_USER = "SYSTEM";
  public static final String PMS_ID_URL = "$";

  public int groupId;
  public String username;
  private byte[] password;
  private byte[] uuid;
  public String title;
  public String url;
  public String additional;

  public PwDate tCreation;
  public PwDate tLastMod;
  public PwDate tLastAccess;
  public PwDate tExpire;

  /** A string describing what is in pBinaryData */
  public String binaryDesc;
  private byte[] binaryData;

  @Override public boolean equals(Object obj) {
    if (!(obj instanceof PwEntryV3)) {
      return false;
    }
    PwEntryV3 otherEntry = (PwEntryV3) obj;
    return icon.equals(otherEntry.icon)
        && username.equals(otherEntry.username)
        && (password.length == otherEntry.password.length && password == otherEntry.password)
        && title.equals(otherEntry.title)
        && url.equals(otherEntry.url)
        && tExpire.equals(otherEntry.tExpire);
  }

  private static Date getDefaultDate() {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, 2004);
    cal.set(Calendar.MONTH, Calendar.JANUARY);
    cal.set(Calendar.DAY_OF_MONTH, 1);
    cal.set(Calendar.HOUR, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);

    return cal.getTime();
  }

  private static Date getNeverExpire() {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, 2999);
    cal.set(Calendar.MONTH, 11);
    cal.set(Calendar.DAY_OF_MONTH, 28);
    cal.set(Calendar.HOUR, 23);
    cal.set(Calendar.MINUTE, 59);
    cal.set(Calendar.SECOND, 59);

    return cal.getTime();
  }

  /**
   * This date was was accidentally being written
   * out when an entry was supposed to be marked as
   * expired. We'll use this to silently correct those
   * entries.
   */
  private static Date getNeverExpireBug() {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, 2999);
    cal.set(Calendar.MONTH, 11);
    cal.set(Calendar.DAY_OF_MONTH, 30);
    cal.set(Calendar.HOUR, 23);
    cal.set(Calendar.MINUTE, 59);
    cal.set(Calendar.SECOND, 59);

    return cal.getTime();
  }

  public static boolean IsNever(Date date) {
    return PwDate.IsSameDate(NEVER_EXPIRE, date);
  }

  // for tree traversing
  public PwGroupV3 parent = null;

  public PwEntryV3() {
    super();
  }

	/*
	public PwEntryV3(PwEntryV3 source) {
		assign(source);
	}
	*/

  public PwEntryV3(PwGroupV3 p) {
    this(p, true, true);
  }

  public PwEntryV3(PwGroupV3 p, boolean initId, boolean initDates) {

    parent = p;
    groupId = ((PwGroupIdV3) parent.getId()).getId();

    if (initId) {
      Random random = new Random();
      uuid = new byte[16];
      random.nextBytes(uuid);
    }

    if (initDates) {
      Calendar cal = Calendar.getInstance();
      Date now = cal.getTime();
      tCreation = new PwDate(now);
      tLastAccess = new PwDate(now);
      tLastMod = new PwDate(now);
      tExpire = new PwDate(NEVER_EXPIRE);
    }
  }

  /**
   * @return the actual password byte array.
   */
  @Override
  public String getPassword(boolean decodeRef, PwDatabase db) {
    if (password == null) {
      return "";
    }

    return new String(password);
  }

  public byte[] getPasswordBytes() {
    return password;
  }

  /**
   * fill byte array
   */
  private static void fill(byte[] array, byte value) {
    for (int i = 0; i < array.length; i++)
      array[i] = value;
    return;
  }

  /** Securely erase old password before copying new. */
  public void setPassword(byte[] buf, int offset, int len) {
    if (password != null) {
      fill(password, (byte) 0);
      password = null;
    }
    password = new byte[len];
    System.arraycopy(buf, offset, password, 0, len);
  }

  @Override
  public void setPassword(String pass, PwDatabase db) {
    byte[] password;
    try {
      password = pass.getBytes("UTF-8");
      setPassword(password, 0, password.length);
    } catch (UnsupportedEncodingException e) {
      assert false;
      password = pass.getBytes();
      setPassword(password, 0, password.length);
    }
  }

  /**
   * @return the actual binaryData byte array.
   */
  public byte[] getBinaryData() {
    return binaryData;
  }

  /** Securely erase old data before copying new. */
  public void setBinaryData(byte[] buf, int offset, int len) {
    if (binaryData != null) {
      fill(binaryData, (byte) 0);
      binaryData = null;
    }
    binaryData = new byte[len];
    System.arraycopy(buf, offset, binaryData, 0, len);
  }

  // Determine if this is a MetaStream entry
  @Override
  public boolean isMetaStream() {
    if (binaryData == null) return false;
    if (additional == null || additional.length() == 0) return false;
    if (!binaryDesc.equals(PMS_ID_BINDESC)) return false;
    if (title == null) return false;
    if (!title.equals(PMS_ID_TITLE)) return false;
    if (username == null) return false;
    if (!username.equals(PMS_ID_USER)) return false;
    if (url == null) return false;
    if (!url.equals(PMS_ID_URL)) return false;
    if (!icon.isMetaStreamIcon()) return false;

    return true;
  }

  @Override
  public void assign(PwEntry source) {

    if (!(source instanceof PwEntryV3)) {
      throw new RuntimeException("DB version mix");
    }

    super.assign(source);

    PwEntryV3 src = (PwEntryV3) source;
    assign(src);
  }

  private void assign(PwEntryV3 source) {
    title = source.title;
    url = source.url;
    groupId = source.groupId;
    username = source.username;
    additional = source.additional;
    uuid = source.uuid;

    int passLen = source.password.length;
    password = new byte[passLen];
    System.arraycopy(source.password, 0, password, 0, passLen);

    tCreation = (PwDate) source.tCreation.clone();
    tLastMod = (PwDate) source.tLastMod.clone();
    tLastAccess = (PwDate) source.tLastAccess.clone();
    tExpire = (PwDate) source.tExpire.clone();

    binaryDesc = source.binaryDesc;

    if (source.binaryData != null) {
      int descLen = source.binaryData.length;
      binaryData = new byte[descLen];
      System.arraycopy(source.binaryData, 0, binaryData, 0, descLen);
    }

    parent = source.parent;
  }

  @Override
  public Object clone() {
    PwEntryV3 newEntry = (PwEntryV3) super.clone();

    if (password != null) {
      int passLen = password.length;
      password = new byte[passLen];
      System.arraycopy(password, 0, newEntry.password, 0, passLen);
    }

    newEntry.tCreation = (PwDate) tCreation.clone();
    newEntry.tLastMod = (PwDate) tLastMod.clone();
    newEntry.tLastAccess = (PwDate) tLastAccess.clone();
    newEntry.tExpire = (PwDate) tExpire.clone();

    newEntry.binaryDesc = binaryDesc;

    if (binaryData != null) {
      int descLen = binaryData.length;
      newEntry.binaryData = new byte[descLen];
      System.arraycopy(binaryData, 0, newEntry.binaryData, 0, descLen);
    }

    newEntry.parent = parent;

    return newEntry;
  }

  @Override
  public Date getLastAccessTime() {
    return tLastAccess.getJDate();
  }

  @Override
  public Date getCreationTime() {
    return tCreation.getJDate();
  }

  @Override
  public Date getExpiryTime() {
    return tExpire.getJDate();
  }

  @Override
  public Date getLastModificationTime() {
    return tLastMod.getJDate();
  }

  @Override
  public void setCreationTime(Date create) {
    tCreation = new PwDate(create);
  }

  @Override
  public void setLastModificationTime(Date mod) {
    tLastMod = new PwDate(mod);
  }

  @Override
  public void setLastAccessTime(Date access) {
    tLastAccess = new PwDate(access);
  }

  @Override
  public void setExpires(boolean expires) {
    if (!expires) {
      tExpire = PW_NEVER_EXPIRE;
    }
  }

  @Override
  public void setExpiryTime(Date expires) {
    tExpire = new PwDate(expires);
  }

  @Override
  public PwGroupV3 getParent() {
    return parent;
  }

  @Override
  public UUID getUUID() {
    return Types.bytestoUUID(uuid);
  }

  @Override
  public void setUUID(UUID u) {
    uuid = Types.UUIDtoBytes(u);
  }

  @Override
  public String getUsername(boolean decodeRef, PwDatabase db) {
    if (username == null) {
      return "";
    }

    return username;
  }

  @Override
  public void setUsername(String user, PwDatabase db) {
    username = user;
  }

  @Override
  public String getTitle(boolean decodeRef, PwDatabase db) {
    return title;
  }

  @Override
  public void setTitle(String title, PwDatabase db) {
    this.title = title;
  }

  @Override
  public String getNotes(boolean decodeRef, PwDatabase db) {
    return additional;
  }

  @Override
  public void setNotes(String notes, PwDatabase db) {
    additional = notes;
  }

  @Override
  public String getUrl(boolean decodeRef, PwDatabase db) {
    return url;
  }

  @Override
  public void setUrl(String url, PwDatabase db) {
    this.url = url;
  }

  @Override
  public boolean expires() {
    return !IsNever(tExpire.getJDate());
  }

  public void populateBlankFields(PwDatabaseV3 db) {
    if (icon == null) {
      icon = db.iconFactory.getIcon(1);
    }

    if (username == null) {
      username = "";
    }

    if (password == null) {
      password = new byte[0];
    }

    if (uuid == null) {
      uuid = Types.UUIDtoBytes(UUID.randomUUID());
    }

    if (title == null) {
      title = "";
    }

    if (url == null) {
      url = "";
    }

    if (additional == null) {
      additional = "";
    }

    if (tCreation == null) {
      tCreation = DEFAULT_PWDATE;
    }

    if (tLastMod == null) {
      tLastMod = DEFAULT_PWDATE;
    }

    if (tLastAccess == null) {
      tLastAccess = DEFAULT_PWDATE;
    }

    if (tExpire == null) {
      tExpire = PW_NEVER_EXPIRE;
    }

    if (binaryDesc == null) {
      binaryDesc = "";
    }

    if (binaryData == null) {
      binaryData = new byte[0];
    }
  }

  @Override
  public void setParent(PwGroup parent) {
    this.parent = (PwGroupV3) parent;
  }
}