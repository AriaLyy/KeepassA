/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database.save;

import java.io.OutputStream;
import java.security.SecureRandom;

import com.keepassdroid.database.PwDatabaseV3;
import com.keepassdroid.database.PwDatabaseV3Debug;
import com.keepassdroid.database.PwDbHeader;
import com.keepassdroid.database.PwDbHeaderV3;
import com.keepassdroid.database.exception.PwDbOutputException;

public class PwDbV3OutputDebug extends PwDbV3Output {
	PwDatabaseV3Debug debugDb;
	private boolean noHeaderHash;

	public PwDbV3OutputDebug(PwDatabaseV3 pm, OutputStream os) {
		this(pm, os, false);
	}

	public PwDbV3OutputDebug(PwDatabaseV3 pm, OutputStream os, boolean noHeaderHash) {
		super(pm, os);
		debugDb = (PwDatabaseV3Debug) pm;
		this.noHeaderHash = noHeaderHash;
	}

	@Override
	protected SecureRandom setIVs(PwDbHeader h) throws PwDbOutputException {
		PwDbHeaderV3 header = (PwDbHeaderV3) h;
		
		
		// Reuse random values to test equivalence in debug mode
		PwDbHeaderV3 origHeader = debugDb.dbHeader;
		System.arraycopy(origHeader.encryptionIV, 0, header.encryptionIV, 0, origHeader.encryptionIV.length);
		System.arraycopy(origHeader.masterSeed, 0, header.masterSeed, 0, origHeader.masterSeed.length);
		System.arraycopy(origHeader.transformSeed, 0, header.transformSeed, 0, origHeader.transformSeed.length);
		
		return null;
	}

	@Override
	protected boolean useHeaderHash() {
		return !noHeaderHash;
	}

}