/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.utils;

import android.net.Uri;

import com.keepassdroid.database.PwDate;
import com.keepassdroid.database.PwEntryV3;

public class EmptyUtils {
	public static boolean isNullOrEmpty(String str) {
		return (str == null) || (str.length() == 0);
	}
	
	public static boolean isNullOrEmpty(byte[] buf) {
		return (buf == null) || (buf.length == 0);
	}
	
	public static boolean isNullOrEmpty(PwDate date) {
		return (date == null) || date.equals(PwEntryV3.DEFAULT_PWDATE);
	}

	public static boolean isNullOrEmpty(Uri uri) {
		return (uri==null) || (uri.toString().length() == 0);
	}
}