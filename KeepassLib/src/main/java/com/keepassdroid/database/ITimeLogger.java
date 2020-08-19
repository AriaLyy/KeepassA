/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database;

import java.util.Date;

public interface ITimeLogger {
	Date getLastModificationTime();
	void setLastModificationTime(Date date);
	
	Date getCreationTime();
	void setCreationTime(Date date);
	
	Date getLastAccessTime();
	void setLastAccessTime(Date date);
	
	Date getExpiryTime();
	void setExpiryTime(Date date);
	
	boolean expires();
	void setExpires(boolean exp);
	
	long getUsageCount();
	void setUsageCount(long count);
	
	Date getLocationChanged();
	void setLocationChanged(Date date);

}