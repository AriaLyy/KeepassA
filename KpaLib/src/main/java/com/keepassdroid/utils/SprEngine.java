/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.utils;

import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwEntry;

public class SprEngine {
	
	private static SprEngineV4 sprV4 = new SprEngineV4();
	private static SprEngine spr = new SprEngine();
	
	public static SprEngine getInstance(PwDatabase db) {
		if (db instanceof PwDatabaseV4) {
            return sprV4;
		} 
		else {
            return spr;
		}
	}
	
	public String compile(String text, PwEntry entry, PwDatabase database) {
		return text;
	}

}