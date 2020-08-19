/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database.security;

import android.util.Log;
import com.keepassdroid.crypto.CipherFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ProtectedBinary {

  private static final String TAG = ProtectedBinary.class.getSimpleName();
  public final static ProtectedBinary EMPTY = new ProtectedBinary();

  private byte[] data;
  private boolean protect;
  private File dataFile;
  private int size;
  private static final SecureRandom secureRandom = new SecureRandom();
  private FileParams fileParams;

  private class FileParams {

    private File dataFile;
    public CipherOutputStream cos;
    public SecretKeySpec keySpec;
    public IvParameterSpec ivSpec;

    public Cipher initCipher(int mode) {
      Cipher cipher;
      try {
        cipher = CipherFactory.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(mode, keySpec, ivSpec);
      } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException(e);
      } catch (NoSuchPaddingException e) {
        throw new IllegalStateException(e);
      } catch (InvalidKeyException e) {
        throw new IllegalStateException(e);
      } catch (InvalidAlgorithmParameterException e) {
        throw new IllegalStateException(e);
      }

      return cipher;
    }

    public void setupEnc(File file) {

      byte[] iv = new byte[16];
      byte[] key = new byte[32];
      secureRandom.nextBytes(key);
      secureRandom.nextBytes(iv);

      keySpec = new SecretKeySpec(key, "AES");
      ivSpec = new IvParameterSpec((iv));

      Cipher cipherOut = initCipher(Cipher.ENCRYPT_MODE);
      FileOutputStream fos;
      try {
        fos = new FileOutputStream(file);
      } catch (FileNotFoundException e) {
        throw new IllegalStateException(e);
      }

      cos = new CipherOutputStream(fos, cipherOut);
    }

    public FileParams(File dataFile) {
      this.dataFile = dataFile;
      setupEnc(dataFile);
    }
  }

  public boolean isProtected() {
    return protect;
  }

  public int length() {
		if (data != null) {
			return data.length;
		}
		if (dataFile != null) {
			return size;
		}
    return 0;
  }

  private ProtectedBinary() {
    this.protect = false;
    this.data = null;
    this.dataFile = null;
    this.size = 0;
  }

  public ProtectedBinary(boolean enableProtection, byte[] data) {
    this.protect = enableProtection;
    this.data = data;
    this.dataFile = null;
		if (data != null) {
			this.size = data.length;
		} else {
			this.size = 0;
		}
  }

  public ProtectedBinary(boolean enableProtection, File dataFile, int size) {
    this.protect = enableProtection;
    this.data = null;
    this.dataFile = dataFile;
    this.size = size;

    fileParams = new FileParams(dataFile);
  }

  public OutputStream getOutputStream() {
    assert (fileParams != null);
    return fileParams.cos;
  }

  public InputStream getData() throws IOException {
		if (data != null) {
			return new ByteArrayInputStream(data);
		} else if (dataFile != null) {
			return new CipherInputStream(new FileInputStream(dataFile),
					fileParams.initCipher(Cipher.DECRYPT_MODE));
		} else {
			return null;
		}
  }

  public void clear() {
    data = null;
		if (dataFile != null && !dataFile.delete()) {
			Log.e(TAG, "Unable to delete temp file " + dataFile.getAbsolutePath());
		}
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ProtectedBinary)){
      return false;
    }
    ProtectedBinary other = (ProtectedBinary) o;
    return this == other
        || getClass() == other.getClass()
        && protect == other.protect
        && size == other.size
        && Arrays.equals(data, other.data)
        && dataFile != null
        && dataFile.equals(other.dataFile);
  }
}