/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.stream;

import java.io.IOException;
import java.io.InputStream;

public class CountInputStream extends InputStream {
	InputStream is;
	long bytes = 0;
	
	public CountInputStream(InputStream is) {
		this.is = is;
	}

	@Override
	public int available() throws IOException {
		return is.available();
	}

	@Override
	public void close() throws IOException {
		is.close();
	}

	@Override
	public void mark(int readlimit) {
		is.mark(readlimit);
	}

	@Override
	public boolean markSupported() {
		return is.markSupported();
	}

	@Override
	public int read() throws IOException {
		bytes++;
		return is.read();
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		bytes += length;
		return is.read(buffer, offset, length);
	}

	@Override
	public int read(byte[] buffer) throws IOException {
		bytes += buffer.length;
		return is.read(buffer);
	}

	@Override
	public synchronized void reset() throws IOException {
		is.reset();
	}

	@Override
	public long skip(long byteCount) throws IOException {
		bytes += byteCount;
		return is.skip(byteCount);
	}

}