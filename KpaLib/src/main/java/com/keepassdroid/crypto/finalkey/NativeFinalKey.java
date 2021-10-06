/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.crypto.finalkey;

import java.io.IOException;

import com.keepassdroid.crypto.NativeLib;


public class NativeFinalKey extends FinalKey {
	
	public static boolean availble() {
		return NativeLib.init();
	}

	@Override
	public byte[] transformMasterKey(byte[] seed, byte[] key, long rounds) throws IOException {
		NativeLib.init();
		
		return nTransformMasterKey(seed, key, rounds);

	}
	
	private static native byte[] nTransformMasterKey(byte[] seed, byte[] key, long rounds);

	// For testing
	/*
	public static byte[] reflect(byte[] key) {
		NativeLib.init();
		
		return nativeReflect(key);
	}
	
	private static native byte[] nativeReflect(byte[] key);
	*/
	

}