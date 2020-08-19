/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.crypto;

import java.security.Provider;

public final class AESProvider extends Provider {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3846349284296062658L;

	public AESProvider() {
		super("AESProvider", 1.0, "");
		put("Cipher.AES",com.keepassdroid.crypto.NativeAESCipherSpi.class.getName());
	}

}