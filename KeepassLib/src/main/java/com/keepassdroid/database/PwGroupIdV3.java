/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database;

public class PwGroupIdV3 extends PwGroupId {

	private int id;
	
	public PwGroupIdV3(int i) {
		id = i;
	}
	
	@Override
	public boolean equals(Object compare) {
		if ( ! (compare instanceof PwGroupIdV3) ) {
			return false;
		}
		
		PwGroupIdV3 cmp = (PwGroupIdV3) compare;
		return id == cmp.id;
	}

	@Override
	public int hashCode() {
		Integer i = Integer.valueOf(id);
		return i.hashCode();
	}
	
	public int getId() {
		return id;
	}
	

}