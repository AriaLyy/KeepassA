/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.utils;

import java.util.HashMap;
import java.util.Map;

import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwEntryV4;

public class SprContextV4 implements Cloneable {
	public PwDatabaseV4 db;
	public PwEntryV4 entry;
	public Map<String, String> refsCache = new HashMap<String, String>();
	
	public SprContextV4(PwDatabaseV4 db, PwEntryV4 entry) {
		this.db = db;
		this.entry = entry;
	}

	@Override
	protected Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}