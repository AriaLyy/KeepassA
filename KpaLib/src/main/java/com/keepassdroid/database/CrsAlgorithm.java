/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database;

public enum CrsAlgorithm {
	
	Null(0),
	ArcFourVariant(1),
	Salsa20(2),
	ChaCha20(3);

	public static final int count = 4;
	public final int id;
	
	private CrsAlgorithm(int num) {
		id = num;
	}

	public static CrsAlgorithm fromId(int num) {
		for ( CrsAlgorithm e : CrsAlgorithm.values() ) {
			if ( e.id == num ) {
				return e;
			}
		}
		
		return null;
	}

}