/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.stream;

import java.io.IOException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HmacBlockStream {
  public static byte[] GetHmacKey64(byte[] key, long blockIndex) {
    MessageDigest hash;
    try {
      hash = MessageDigest.getInstance("SHA-512");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    NullOutputStream nos = new NullOutputStream();
    DigestOutputStream dos = new DigestOutputStream(nos, hash);
    LEDataOutputStream leos = new LEDataOutputStream(dos);

    try {
      leos.writeLong(blockIndex);
      leos.write(key);
      leos.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    byte[] hashKey = hash.digest();
    assert (hashKey.length == 64);

    return hashKey;
  }
}