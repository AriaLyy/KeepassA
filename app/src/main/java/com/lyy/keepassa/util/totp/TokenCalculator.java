/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util.totp;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import timber.log.Timber;

/**
 * 地址：https://github.com/andOTP/andOTP.git
 */
public class TokenCalculator {
  public static final int TOTP_DEFAULT_PERIOD = 30;
  public static final int TOTP_DEFAULT_DIGITS = 6;
  public static final int HOTP_INITIAL_COUNTER = 1;
  public static final int STEAM_DEFAULT_DIGITS = 5;

  private static final char[] STEAMCHARS = new char[] {
      '2', '3', '4', '5', '6', '7', '8', '9', 'B', 'C',
      'D', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'P', 'Q',
      'R', 'T', 'V', 'W', 'X', 'Y'
  };

  public enum HashAlgorithm {
    SHA1, SHA256, SHA512
  }

  public static final HashAlgorithm DEFAULT_ALGORITHM = HashAlgorithm.SHA1;

  private static byte[] generateHash(HashAlgorithm algorithm, byte[] key, byte[] data)
      throws NoSuchAlgorithmException, InvalidKeyException {
    String algo = "Hmac" + algorithm.toString();

    Mac mac = Mac.getInstance(algo);
    mac.init(new SecretKeySpec(key, algo));

    return mac.doFinal(data);
  }

  public static int TOTP_RFC6238(byte[] secret, int period, long time, int digits,
      HashAlgorithm algorithm) {
    int fullToken = TOTP(secret, period, time, algorithm);
    int div = (int) Math.pow(10, digits);

    return fullToken % div;
  }

  /**
   * TOTP_RFC6238 协议的TOTP
   *
   * @param secret seed
   * @param period {@link #TOTP_DEFAULT_PERIOD}、
   * @param digits {@link #TOTP_DEFAULT_DIGITS}、{@link #STEAM_DEFAULT_DIGITS}
   * @param algorithm {@link HashAlgorithm}
   */
  public static String TOTP_RFC6238(byte[] secret, int period, int digits,
      HashAlgorithm algorithm) {
    return formatTokenString(
        TOTP_RFC6238(secret, period, System.currentTimeMillis() / 1000, digits, algorithm), digits);
  }

  /**
   * TOTP_RFC6238 协议的TOTP
   *
   * @param secret seed
   * @param period {@link #TOTP_DEFAULT_PERIOD}、
   * @param digits {@link #TOTP_DEFAULT_DIGITS}、{@link #STEAM_DEFAULT_DIGITS}
   * @param algorithm {@link HashAlgorithm}
   */
  public static String TOTP_Steam(byte[] secret, int period, int digits, HashAlgorithm algorithm) {
    int fullToken = TOTP(secret, period, System.currentTimeMillis() / 1000, algorithm);

    StringBuilder tokenBuilder = new StringBuilder();

    for (int i = 0; i < digits; i++) {
      tokenBuilder.append(STEAMCHARS[fullToken % STEAMCHARS.length]);
      fullToken /= STEAMCHARS.length;
    }

    return tokenBuilder.toString();
  }

  /**
   * TOTP_RFC6238 协议的HOTP
   *
   * @param secret seed
   * @param counter {@link #HOTP_INITIAL_COUNTER}
   * @param digits {@link #TOTP_DEFAULT_DIGITS}、{@link #STEAM_DEFAULT_DIGITS}
   * @param algorithm {@link HashAlgorithm}
   */
  public static String HOTP(byte[] secret, long counter, int digits, HashAlgorithm algorithm) {
    int fullToken = HOTP(secret, counter, algorithm);
    int div = (int) Math.pow(10, digits);

    return formatTokenString(fullToken % div, digits);
  }

  private static int TOTP(byte[] key, int period, long time, HashAlgorithm algorithm) {
    return HOTP(key, time / period, algorithm);
  }

  private static int HOTP(byte[] key, long counter, HashAlgorithm algorithm) {
    int r = 0;

    try {
      byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
      byte[] hash = generateHash(algorithm, key, data);

      int offset = hash[hash.length - 1] & 0xF;

      int binary = (hash[offset] & 0x7F) << 0x18;
      binary |= (hash[offset + 1] & 0xFF) << 0x10;
      binary |= (hash[offset + 2] & 0xFF) << 0x08;
      binary |= (hash[offset + 3] & 0xFF);

      r = binary;
    } catch (Exception e) {
      Timber.e(e);
    }

    return r;
  }

  private static String formatTokenString(int token, int digits) {
    NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
    numberFormat.setMinimumIntegerDigits(digits);
    numberFormat.setGroupingUsed(false);

    return numberFormat.format(token);
  }

  public static String formatToken(String s, int chunkSize) {
    if (chunkSize == 0 || s == null) {
      return s;
    }

    StringBuilder ret = new StringBuilder("");
    int index = s.length();
    while (index > 0) {
      ret.insert(0, s.substring(Math.max(index - chunkSize, 0), index));
      ret.insert(0, " ");
      index = index - chunkSize;
    }
    return ret.toString().trim();
  }
}