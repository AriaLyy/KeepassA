/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database;

import java.util.UUID;

public class PwGroupIdV4 extends PwGroupId {
	private UUID uuid;
	
	public PwGroupIdV4(UUID u) {
		uuid = u;
	}
	
	@Override
	public boolean equals(Object id) {
		if ( ! (id instanceof PwGroupIdV4) ) {
			return false;
		}
		
		PwGroupIdV4 v4 = (PwGroupIdV4) id;
		
		return uuid.equals(v4.uuid);
	}
	
	@Override
	public int hashCode() {
		return uuid.hashCode();
	}
	
	public UUID getId() {
		return uuid;
	}

}