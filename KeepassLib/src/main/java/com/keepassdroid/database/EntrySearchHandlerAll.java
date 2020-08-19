/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database;

import java.util.Date;
import java.util.List;

public class EntrySearchHandlerAll extends EntryHandler<PwEntry> {
	private List<PwEntry> listStorage;
	private SearchParameters sp;
	private Date now;
	
	public EntrySearchHandlerAll(SearchParameters sp, List<PwEntry> listStorage) {
		this.sp = sp;
		this.listStorage = listStorage;
		now = new Date();
	}

	@Override
	public boolean operate(PwEntry entry) {
		if (sp.respectEntrySearchingDisabled && !entry.isSearchingEnabled()) {
			return true;
		}
		
		if (sp.excludeExpired && entry.expires() && now.after(entry.getExpiryTime())) {
			return true;
		}
		
		listStorage.add(entry);
		return true;
	}

}