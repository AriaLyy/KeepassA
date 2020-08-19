/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.stream;

import java.io.IOException;
import java.io.OutputStream;

public class CountOutputStream extends OutputStream {
	OutputStream os;
	long bytes = 0;
	
	public CountOutputStream(OutputStream os) {
		this.os = os;
	}


	@Override
	public void close() throws IOException {
		os.close();
	}

	@Override
	public void flush() throws IOException {
		os.flush();
	}


	@Override
	public void write(byte[] buffer, int offset, int count) throws IOException {
		bytes += count;
		os.write(buffer, offset, count);
	}

	@Override
	public void write(byte[] buffer) throws IOException {
		bytes += buffer.length;
		os.write(buffer);
	}

	@Override
	public void write(int oneByte) throws IOException {
		bytes++;
		os.write(oneByte);
	}
}