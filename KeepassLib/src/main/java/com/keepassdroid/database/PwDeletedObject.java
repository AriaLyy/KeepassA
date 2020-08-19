/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database;

import java.util.Date;
import java.util.UUID;

public class PwDeletedObject {
	public UUID uuid;
	private Date deletionTime;
	
	public PwDeletedObject() {
		
	}
	
	public PwDeletedObject(UUID u) {
		this(u, new Date());
	}
	
	public PwDeletedObject(UUID u, Date d) {
		uuid = u;
		deletionTime = d;
	}
	
	public Date getDeletionTime() {
		if ( deletionTime == null ) {
			return new Date(System.currentTimeMillis());
		}
		
		return deletionTime;
	}
	
	public void setDeletionTime(Date date) {
		deletionTime = date;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		else if (o == null) {
			return false;
		}
		else if (!(o instanceof PwDeletedObject)) {
			return false;
		}
		
		PwDeletedObject rhs = (PwDeletedObject) o;
		
		return uuid.equals(rhs.uuid);
	}
}