/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database.save;

import java.io.IOException;
import java.io.OutputStream;


import com.keepassdroid.database.PwDbHeaderV3;
import com.keepassdroid.stream.LEDataOutputStream;

public class PwDbHeaderOutputV3 {
	private PwDbHeaderV3 mHeader;
	private OutputStream mOS;
	
	public PwDbHeaderOutputV3(PwDbHeaderV3 header, OutputStream os) {
		mHeader = header;
		mOS = os;
	}
	
	public void outputStart() throws IOException {
		mOS.write(LEDataOutputStream.writeIntBuf(mHeader.signature1));
		mOS.write(LEDataOutputStream.writeIntBuf(mHeader.signature2));
		mOS.write(LEDataOutputStream.writeIntBuf(mHeader.flags));
		mOS.write(LEDataOutputStream.writeIntBuf(mHeader.version));
		mOS.write(mHeader.masterSeed);
		mOS.write(mHeader.encryptionIV);
		mOS.write(LEDataOutputStream.writeIntBuf(mHeader.numGroups));
		mOS.write(LEDataOutputStream.writeIntBuf(mHeader.numEntries));
	}
	
	public void outputContentHash() throws IOException {
		mOS.write(mHeader.contentsHash);
	}
	
	public void outputEnd() throws IOException {
		mOS.write(mHeader.transformSeed);
		mOS.write(LEDataOutputStream.writeIntBuf(mHeader.numKeyEncRounds));
	}
	
	public void output() throws IOException {
		outputStart();
		outputContentHash();
		outputEnd();
	}
	
	public void close() throws IOException {
		mOS.close();
	}
}