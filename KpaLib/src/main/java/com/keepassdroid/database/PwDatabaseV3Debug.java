/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database;

public class PwDatabaseV3Debug extends PwDatabaseV3 {
	public byte[] postHeader;
	public PwDbHeaderV3 dbHeader;
	
	@Override
	public void copyEncrypted(byte[] buf, int offset, int size) {
		postHeader = new byte[size];
		System.arraycopy(buf, offset, postHeader, 0, size);
	}
	@Override
	public void copyHeader(PwDbHeaderV3 header) {
		dbHeader = header;
	}
}