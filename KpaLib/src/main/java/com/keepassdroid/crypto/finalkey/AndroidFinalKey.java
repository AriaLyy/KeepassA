/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.crypto.finalkey;

import android.annotation.SuppressLint;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

public class AndroidFinalKey extends FinalKey {

	@SuppressLint("GetInstance")
	@Override
	public byte[] transformMasterKey(byte[] pKeySeed, byte[] pKey, long rounds) throws IOException {
		Cipher cipher;
		try {
			cipher = Cipher.getInstance("AES/ECB/NoPadding");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("NoSuchAlgorithm: " + e.getMessage());
		} catch (NoSuchPaddingException e) {
			throw new IOException("NoSuchPadding: " + e.getMessage());
		}

		try {
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(pKeySeed, "AES"));
		} catch (InvalidKeyException e) {
			throw new IOException("InvalidPasswordException: " + e.getMessage());
		}

		// Encrypt key rounds times
		byte[] newKey = new byte[pKey.length];
		System.arraycopy(pKey, 0, newKey, 0, pKey.length);
		byte[] destKey = new byte[pKey.length];
		for (int i = 0; i < rounds; i++) {
			try {
				cipher.update(newKey, 0, newKey.length, destKey, 0);
				System.arraycopy(destKey, 0, newKey, 0, newKey.length);

			} catch (ShortBufferException e) {
				throw new IOException("Short buffer: " + e.getMessage());
			}
		}

		// Hash the key
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			assert true;
			throw new IOException("SHA-256 not implemented here: " + e.getMessage());
		}

		md.update(newKey);
		return md.digest();
	}

}