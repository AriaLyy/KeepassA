/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database;

public class SearchParametersV4 extends SearchParameters implements Cloneable {
	public static SearchParametersV4 DEFAULT = new SearchParametersV4();
	
	public boolean searchInOther = true;
	public boolean searchInUUIDs = false;
	public boolean searchInTags = true;

	@Override
	public Object clone() {
		return super.clone();
	}

	@Override
	public void setupNone() {
		super.setupNone();
		searchInOther = false;
		searchInUUIDs = false;
		searchInTags = false;
	}
}