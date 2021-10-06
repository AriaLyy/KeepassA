/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database.iterator;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import com.keepassdroid.database.PwEntryV4;
import com.keepassdroid.database.SearchParametersV4;
import com.keepassdroid.database.security.ProtectedString;

public class EntrySearchStringIteratorV4 extends EntrySearchStringIterator {
	
	private String current;
	private Iterator<Entry<String, ProtectedString>> setIterator;
	private SearchParametersV4 sp;

	public EntrySearchStringIteratorV4(PwEntryV4 entry) {
		this.sp = SearchParametersV4.DEFAULT;
		setIterator = entry.strings.entrySet().iterator();
		advance();
		
	}

	public EntrySearchStringIteratorV4(PwEntryV4 entry, SearchParametersV4 sp) {
		this.sp = sp;
		setIterator = entry.strings.entrySet().iterator();
		advance();
	}

	@Override
	public boolean hasNext() {
		return current != null;
	}

	@Override
	public String next() {
		if (current == null) {
			throw new NoSuchElementException("Past the end of the list.");
		}
		
		String next = current;
		advance();
		return next;
	}
	
	private void advance() {
		while (setIterator.hasNext()) {
			Entry<String, ProtectedString> entry = setIterator.next();
			
			String key = entry.getKey();
			
			if (searchInField(key)) {
				current = entry.getValue().toString();
				return;
			}
			
		}
		
		current = null;
	}
	
	private boolean searchInField(String key) {
		if (key.equals(PwEntryV4.STR_TITLE)) {
			return sp.searchInTitles;
		} else if (key.equals(PwEntryV4.STR_USERNAME)) {
			return sp.searchInUserNames;
		} else if (key.equals(PwEntryV4.STR_PASSWORD)) {
			return sp.searchInPasswords;
		} else if (key.equals(PwEntryV4.STR_URL)) {
			return sp.searchInUrls;
		} else if (key.equals(PwEntryV4.STR_NOTES)) {
			return sp.searchInNotes;
		} else {
			return sp.searchInOther;
		}
	}

}