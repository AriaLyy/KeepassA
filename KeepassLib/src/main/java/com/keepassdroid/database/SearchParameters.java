/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database;

/**
 * @author bpellin
 * Parameters for searching strings in the database.
 *
 */
public class SearchParameters implements Cloneable {
	public static final SearchParameters DEFAULT = new SearchParameters();
	
	public String searchString;
	
	public boolean regularExpression = false;
	public boolean searchInTitles = true;
	public boolean searchInUserNames = true;
	public boolean searchInPasswords = false;
	public boolean searchInUrls = true;
	public boolean searchInGroupNames = false;
	public boolean searchInNotes = true;
	public boolean ignoreCase = true;
	public boolean ignoreExpired = false;
	public boolean respectEntrySearchingDisabled = true;
	public boolean excludeExpired = false;
	
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
	public void setupNone() {
		searchInTitles = false;
		searchInUserNames = false;
		searchInPasswords = false;
		searchInUrls = false;
		searchInGroupNames = false;
		searchInNotes = false;
	}
}