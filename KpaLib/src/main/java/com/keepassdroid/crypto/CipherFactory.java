/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.crypto;

import android.os.Build;
import com.keepassdroid.crypto.engine.AesEngine;
import com.keepassdroid.crypto.engine.ChaCha20Engine;
import com.keepassdroid.crypto.engine.CipherEngine;
import com.keepassdroid.crypto.engine.TwofishEngine;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import org.spongycastle.jce.provider.BouncyCastleProvider;

public class CipherFactory {
  private static boolean blacklistInit = false;
  private static boolean blacklisted;

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public static Cipher getInstance(String transformation)
      throws NoSuchAlgorithmException, NoSuchPaddingException {
    return getInstance(transformation, false);
  }

  public static Cipher getInstance(String transformation, boolean androidOverride)
      throws NoSuchAlgorithmException, NoSuchPaddingException {
    // Return the native AES if it is possible
    if ((!deviceBlacklisted())
        && (!androidOverride)
        && hasNativeImplementation(transformation)
        && NativeLib.loaded()) {
      return Cipher.getInstance(transformation, new AESProvider());
    } else {
      return Cipher.getInstance(transformation);
    }
  }

  public static boolean deviceBlacklisted() {
    if (!blacklistInit) {
      blacklistInit = true;

      // The Acer Iconia A500 is special and seems to always crash in the native crypto libraries
      blacklisted = Build.MODEL.equals("A500");
    }
    return blacklisted;
  }

  private static boolean hasNativeImplementation(String transformation) {
    return transformation.equals("AES/CBC/PKCS5Padding");
  }

  /**
   * Generate appropriate cipher based on KeePass 2.x UUID's
   */
  public static CipherEngine getInstance(UUID uuid) throws NoSuchAlgorithmException {
    if (uuid.equals(AesEngine.CIPHER_UUID)) {
      return new AesEngine();
    } else if (uuid.equals(TwofishEngine.CIPHER_UUID)) {
      return new TwofishEngine();
    } else if (uuid.equals(ChaCha20Engine.CIPHER_UUID)) {
      return new ChaCha20Engine();
    }

    throw new NoSuchAlgorithmException("UUID unrecognized.");
  }
}