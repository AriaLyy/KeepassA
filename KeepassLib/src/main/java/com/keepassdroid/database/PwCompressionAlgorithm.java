/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database;

public enum PwCompressionAlgorithm {
	
	None(0),
	Gzip(1);
	
	// Note: We can get away with using int's to store unsigned 32-bit ints
	//       since we won't do arithmetic on these values (also unlikely to
	//       reach negative ids).
	public final int id;
	public static final int count = 2;
	
	private PwCompressionAlgorithm(int num) {
		id = num;
	}
	
	public static PwCompressionAlgorithm fromId(int num) {
		for ( PwCompressionAlgorithm e : PwCompressionAlgorithm.values() ) {
			if ( e.id == num ) {
				return e;
			}
		}
		
		return null;
	}
	
}