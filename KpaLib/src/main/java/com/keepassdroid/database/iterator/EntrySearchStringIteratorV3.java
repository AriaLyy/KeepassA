/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database.iterator;

import java.util.NoSuchElementException;

import com.keepassdroid.database.PwEntryV3;
import com.keepassdroid.database.SearchParameters;

public class EntrySearchStringIteratorV3 extends EntrySearchStringIterator {
	
	private PwEntryV3 entry; 
	private SearchParameters sp;
	
	public EntrySearchStringIteratorV3(PwEntryV3 entry) {
		this.entry = entry;
		this.sp = SearchParameters.DEFAULT;
	}

	public EntrySearchStringIteratorV3(PwEntryV3 entry, SearchParameters sp) {
		this.entry = entry;
		this.sp = sp;
	}

	private static final int title = 0;
	private static final int url = 1;
	private static final int username = 2;
	private static final int comment = 3;
	private static final int maxEntries = 4;
	
	private int current = 0;
	
	@Override
	public boolean hasNext() {
		return current < maxEntries;
	}
	
	@Override
	public String next() {
		// Past the end of the list
		if (current == maxEntries) {
			throw new NoSuchElementException("Past final string");
		}
		
        useSearchParameters();
		
		String str = getCurrentString();
		current++;
		return str;
	}
	
	private void useSearchParameters() {
		
		if (sp == null) { return; }

		boolean found = false;
		
		while (!found) {
            switch (current) {
            case title:
                found = sp.searchInTitles;
                break;
            
            case url:
            	found = sp.searchInUrls;
                break;
                    
            case username:
                found = sp.searchInUserNames;    
                break;
            	
            case comment:
            	found = sp.searchInNotes;
                break;
                    
            default:
            	found = true;
            }
                
            if (!found) { current++; }   
		}
	}
	
	private String getCurrentString() {
		switch (current) {
		case title:
			return entry.getTitle();
		
		case url:
			return entry.getUrl();
			
		case username:
			return entry.getUsername();
			
		case comment:
			return entry.getNotes();
			
		default:
			return "";
		}
	}

}