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
import java.io.OutputStream;

/**
 * This class copies everything pulled through its input stream into the 
 * output stream. 
 */
public class CopyInputStream extends InputStream {
	private InputStream is;
	private OutputStream os;
	
	public CopyInputStream(InputStream is, OutputStream os) {
		this.is = is;
		this.os = os;
	}

	@Override
	public int available() throws IOException {
		return is.available();
	}

	@Override
	public void close() throws IOException {
		is.close();
		os.close();
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
		int data = is.read();
		
		if (data != -1) {
			os.write(data);
		}
		
		return data;
	}

	@Override
	public int read(byte[] b, int offset, int length) throws IOException {
		int len = is.read(b, offset, length);
		
		if (len != -1) {
			os.write(b, offset, len);
		}
		
		return len;
	}

	@Override
	public int read(byte[] b) throws IOException {
		int len = is.read(b);
		
		if (len != -1) {
			os.write(b, 0, len);
		}
		
		return len;
	}

	@Override
	public synchronized void reset() throws IOException {
		is.reset();
	}

	@Override
	public long skip(long byteCount) throws IOException {
		return is.skip(byteCount);
	}

}