/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database;


/** "Delegate" class for operating on each group when traversing all of
 * them
 * @author bpellin
 *
 */
public abstract class GroupHandler<T> {
	public abstract boolean operate(T entry);
}