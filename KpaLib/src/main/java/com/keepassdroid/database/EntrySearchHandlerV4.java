/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database;

import java.util.List;
import java.util.Locale;

import com.keepassdroid.utils.StrUtil;
import com.keepassdroid.utils.UuidUtil;

public class EntrySearchHandlerV4 extends EntrySearchHandler {
	private SearchParametersV4 sp;

	protected EntrySearchHandlerV4(SearchParameters sp, List<PwEntry> listStorage) {
		super(sp, listStorage);
		this.sp = (SearchParametersV4) sp;
	}

	@Override
	protected boolean searchID(PwEntry e) {
		PwEntryV4 entry = (PwEntryV4) e;
		if (sp.searchInUUIDs) {
			String hex = UuidUtil.toHexString(entry.uuid);
			return StrUtil.indexOfIgnoreCase(hex, sp.searchString, Locale.ENGLISH) >= 0;
		}
		
		return false;
	}

	
}