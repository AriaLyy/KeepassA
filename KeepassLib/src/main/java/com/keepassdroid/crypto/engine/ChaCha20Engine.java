/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.crypto.engine;

import com.keepassdroid.utils.Types;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.spongycastle.jce.provider.BouncyCastleProvider;

public class ChaCha20Engine extends CipherEngine {
  public static final UUID CIPHER_UUID = Types.bytestoUUID(
      new byte[] {
          (byte) 0xD6, (byte) 0x03, (byte) 0x8A, (byte) 0x2B, (byte) 0x8B, (byte) 0x6F,
          (byte) 0x4C, (byte) 0xB5, (byte) 0xA5, (byte) 0x24, (byte) 0x33, (byte) 0x9A, (byte) 0x31,
          (byte) 0xDB, (byte) 0xB5, (byte) 0x9A
      });

  @Override
  public int ivLength() {
    return 12;
  }

  @Override
  public Cipher getCipher(int opmode, byte[] key, byte[] IV, boolean androidOverride)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
      InvalidAlgorithmParameterException {
    Cipher cipher = Cipher.getInstance("Chacha7539", new BouncyCastleProvider());
    cipher.init(opmode, new SecretKeySpec(key, "ChaCha7539"), new IvParameterSpec(IV));
    return cipher;
  }
}