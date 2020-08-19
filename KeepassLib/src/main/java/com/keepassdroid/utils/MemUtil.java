/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MemUtil {
	public static byte[] decompress(byte[] input) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(input);
		GZIPInputStream gzis = new GZIPInputStream(bais);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Util.copyStream(gzis, baos);
		
		return baos.toByteArray();
	}
	
	public static byte[] compress(byte[] input) throws IOException {		
		ByteArrayInputStream bais = new ByteArrayInputStream(input);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gzos = new GZIPOutputStream(baos);
		Util.copyStream(bais, gzos);
		gzos.close();
		
		return baos.toByteArray();
	}

}