/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.crypto;

public class NativeLib {
	private static boolean isLoaded = false;
	private static boolean loadSuccess = false;
	
	public static boolean loaded() {
		return init();
	}
	
	public static boolean init() {
		if ( ! isLoaded ) {
			try {
				System.loadLibrary("final-key");
				System.loadLibrary("argon2");
			} catch ( UnsatisfiedLinkError e) {
				return false;
			}
			isLoaded = true;
			loadSuccess = true;
		}
		
		return loadSuccess;
		
	}

}