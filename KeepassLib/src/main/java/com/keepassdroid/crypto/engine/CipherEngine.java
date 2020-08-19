/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.crypto.engine;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

public abstract class CipherEngine {
  public int keyLength() {
    return 32;
  }

  public int ivLength() {
    return 16;
  }

  public abstract Cipher getCipher(int opmode, byte[] key, byte[] IV, boolean androidOverride)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
      InvalidAlgorithmParameterException;

  public Cipher getCipher(int opmode, byte[] key, byte[] IV)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
      InvalidAlgorithmParameterException {
    return getCipher(opmode, key, IV, false);
  }
}