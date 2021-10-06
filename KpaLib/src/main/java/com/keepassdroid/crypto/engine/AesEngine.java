/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.crypto.engine;

import com.keepassdroid.crypto.CipherFactory;
import com.keepassdroid.utils.Types;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesEngine extends CipherEngine {
  public static final UUID CIPHER_UUID = Types.bytestoUUID(
      new byte[] {
          (byte) 0x31, (byte) 0xC1, (byte) 0xF2, (byte) 0xE6, (byte) 0xBF, (byte) 0x71, (byte) 0x43,
          (byte) 0x50,
          (byte) 0xBE, (byte) 0x58, (byte) 0x05, (byte) 0x21, (byte) 0x6A, (byte) 0xFC, (byte) 0x5A,
          (byte) 0xFF
      });

  @Override
  public Cipher getCipher(int opmode, byte[] key, byte[] IV, boolean androidOverride)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
      InvalidAlgorithmParameterException {
    Cipher cipher = CipherFactory.getInstance("AES/CBC/PKCS5Padding", androidOverride);

    cipher.init(opmode, new SecretKeySpec(key, "AES"), new IvParameterSpec(IV));

    return cipher;
  }
}