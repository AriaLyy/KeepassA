/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database;

public abstract class PwDbHeader {

	public static final int PWM_DBSIG_1 = 0x9AA2D903;

	/** Seed that gets hashed with the userkey to form the final key */
	public byte masterSeed[];

	/** IV used for content encryption */
	public byte encryptionIV[] = new byte[16];
	
}