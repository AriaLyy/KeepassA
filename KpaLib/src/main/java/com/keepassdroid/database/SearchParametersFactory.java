/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database;

public class SearchParametersFactory {
	public static SearchParameters getNone(PwDatabase db) {
		SearchParameters sp = getInstance(db);
		sp.setupNone();
		
		return sp;
	}
	
	public static SearchParameters getInstance(PwDatabase db) {
		if (db instanceof PwDatabase) {
			return new SearchParametersV4();
		}
		else {
			return new SearchParameters();
		}
	}
	
	public static SearchParameters getInstance(PwGroup group) {
		if (group instanceof PwGroupV4) {
			return new SearchParametersV4();
		}
		else {
			return new SearchParameters();
		}
	}
}