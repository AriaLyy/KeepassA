/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.utils;

import java.util.UUID;

public class UuidUtil {
	public static String toHexString(UUID uuid) {
		if (uuid == null) { return null; }
		
		byte[] buf = Types.UUIDtoBytes(uuid);
		if (buf == null) { return null; }
		
		int len = buf.length;
		if (len == 0) { return ""; }
		
		StringBuilder sb = new StringBuilder();
		
		short bt;
		char high, low;
		for (int i = 0; i < len; i++) {
			bt = (short)(buf[i] & 0xFF);
			high = (char)(bt >>> 4);
			
		
			low = (char)(bt & 0x0F);
			
			char h,l;
			h = byteToChar(high);
			l = byteToChar(low);

			sb.append(byteToChar(high));
			sb.append(byteToChar(low));
		}
		
		return sb.toString();
	}
	
	// Use short to represent unsigned byte
	private static char byteToChar(char bt) {
        if (bt >= 10) {
            return (char)('A' + bt - 10);
        }
        else {
            return (char)('0' + bt);
        }
	}
}