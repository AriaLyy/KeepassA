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

public class NullOutputStream extends OutputStream {

	@Override
	public void close() throws IOException {
		super.close();
	}

	@Override
	public void flush() throws IOException {
		super.flush();
	}

	@Override
	public void write(byte[] buffer, int offset, int count) throws IOException {
		super.write(buffer, offset, count);
	}

	@Override
	public void write(byte[] buffer) throws IOException {
		super.write(buffer);
	}

	@Override
	public void write(int oneByte) throws IOException {
	}

}