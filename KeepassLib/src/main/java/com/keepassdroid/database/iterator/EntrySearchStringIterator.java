/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database.iterator;

import java.util.Iterator;

import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwEntryV3;
import com.keepassdroid.database.PwEntryV4;
import com.keepassdroid.database.SearchParameters;
import com.keepassdroid.database.SearchParametersV4;

public abstract class EntrySearchStringIterator implements Iterator<String> {
	
	public static EntrySearchStringIterator getInstance(PwEntry e) {
		if (e instanceof PwEntryV3) {
			return new EntrySearchStringIteratorV3((PwEntryV3) e);
		} else if (e instanceof PwEntryV4) {
			return new EntrySearchStringIteratorV4((PwEntryV4) e);
		} else {
			throw new RuntimeException("This should not be possible");
		}
	}
	
	public static EntrySearchStringIterator getInstance(PwEntry e, SearchParameters sp) {
		if (e instanceof PwEntryV3) {
			return new EntrySearchStringIteratorV3((PwEntryV3) e, sp);
		} else if (e instanceof PwEntryV4) {
			return new EntrySearchStringIteratorV4((PwEntryV4) e, (SearchParametersV4) sp);
		} else {
			throw new RuntimeException("This should not be possible");
		}
	}
	
	@Override
	public abstract boolean hasNext();

	@Override
	public abstract String next();

	@Override
	public void remove() {
		throw new UnsupportedOperationException("This iterator cannot be used to remove strings.");
		
	}
	

}