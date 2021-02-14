/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.utils;

import com.keepassdroid.stream.LEDataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * Tools for slicing and dicing Java and KeePass data types.
 *
 * @author Bill Zwicky <wrzwicky@pobox.com>
 */
public class Types {

  public static long ULONG_MAX_VALUE = -1;

  public static long parseVersion(String ver) {
    if (ver == null) {
      return 0;
    }

    String[] verArray = ver.split("[.,]");

    int len = verArray.length;
    if (len <=0) {
      return 0;
    }

    try {
      int part = Integer.parseInt(verArray[0].trim());
      long version = (long)part << 48;

      if (len >=2) {
        part = Integer.parseInt(verArray[1].trim());
        version |= ((long)part << 32);
      }

      if (len >=3) {
        part = Integer.parseInt(verArray[2].trim());
        version |= ((long)part << 16);
      }

      if (len >=4) {
        part = Integer.parseInt(verArray[3].trim());
        version |= (long)part;
      }

      return version;
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  /** Read an unsigned byte */
  public static int readUByte(byte[] buf, int offset) {
    return ((int) buf[offset] & 0xFF);
  }

  /**
   * Write an unsigned byte
   */
  public static void writeUByte(int val, byte[] buf, int offset) {
    buf[offset] = (byte) (val & 0xFF);
  }

  public static byte writeUByte(int val) {
    byte[] buf = new byte[1];

    writeUByte(val, buf, 0);

    return buf[0];
  }

  /**
   * Return len of null-terminated string (i.e. distance to null)
   * within a byte buffer.
   */
  public static int strlen(byte[] buf, int offset) {
    int len = 0;
    while (buf[offset + len] != 0) {
      len++;
    }
    return len;
  }

  /**
   * Copy a sequence of bytes into a new array.
   *
   * @param b - source array
   * @param offset - first byte
   * @param len - number of bytes
   * @return new byte[len]
   */
  public static byte[] extract(byte[] b, int offset, int len) {
    byte[] b2 = new byte[len];
    System.arraycopy(b, offset, b2, 0, len);
    return b2;
  }

  private static final byte[] CRLFbuf = { 0x0D, 0x0A };
  private static final String CRLF = new String(CRLFbuf);
  private static final String SEP = System.getProperty("line.separator");
  private static final boolean REPLACE = !SEP.equals(CRLF);

  public static String readCString(byte[] buf, int offset) throws UnsupportedEncodingException {
    String jstring = new String(buf, offset, strlen(buf, offset), "UTF-8");

    if (REPLACE) {
      jstring = jstring.replace(CRLF, SEP);
    }

    return jstring;
  }

  public static int writeCString(String str, OutputStream os) throws IOException {
    if (str == null) {
      // Write out a null character
      os.write(LEDataOutputStream.writeIntBuf(1));
      os.write(0x00);
      return 0;
    }

    if (REPLACE) {
      str = str.replace(SEP, CRLF);
    }

    byte[] initial = str.getBytes("UTF-8");

    int length = initial.length + 1;
    os.write(LEDataOutputStream.writeIntBuf(length));
    os.write(initial);
    os.write(0x00);

    return length;
  }

  public static UUID bytestoUUID(byte[] buf) {
    return bytestoUUID(buf, 0);
  }

  public static UUID bytestoUUID(byte[] buf, int offset) {
    long lsb = 0;
    for (int i = 15; i >= 8; i--) {
      lsb = (lsb << 8) | (buf[i + offset] & 0xff);
    }

    long msb = 0;
    for (int i = 7; i >= 0; i--) {
      msb = (msb << 8) | (buf[i + offset] & 0xff);
    }

    return new UUID(msb, lsb);
  }

  public static byte[] UUIDtoBytes(UUID uuid) {
    byte[] buf = new byte[16];

    LEDataOutputStream.writeLong(uuid.getMostSignificantBits(), buf, 0);
    LEDataOutputStream.writeLong(uuid.getLeastSignificantBits(), buf, 8);

    return buf;
  }
}