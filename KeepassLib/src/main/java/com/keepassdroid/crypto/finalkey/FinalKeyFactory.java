/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.crypto.finalkey;

import com.keepassdroid.crypto.CipherFactory;

public class FinalKeyFactory {
	public static FinalKey createFinalKey() {
		return createFinalKey(false);
	}
	
	public static FinalKey createFinalKey(boolean androidOverride) {
		// Prefer the native final key implementation
		if ( !CipherFactory.deviceBlacklisted() && !androidOverride && NativeFinalKey.availble() ) {
			return new NativeFinalKey();
		} else {
			// Fall back on the android crypto implementation
			return new AndroidFinalKey();
		}
	}
}