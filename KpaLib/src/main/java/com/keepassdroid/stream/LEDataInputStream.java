/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.keepassdroid.stream;

import com.keepassdroid.utils.Types;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;


/** Little endian version of the DataInputStream
 * @author bpellin
 *
 */
public class LEDataInputStream extends InputStream {

	public static final long INT_TO_LONG_MASK = 0xffffffffL;

	private InputStream baseStream;

	public LEDataInputStream(InputStream in) {
		baseStream = in;
	}

	/** Read a 32-bit value and return it as a long, so that it can
	 *  be interpreted as an unsigned integer.
	 * @return
	 * @throws IOException
	 */
	public long readUInt() throws IOException {
		return readUInt(baseStream);
	}

	public int readInt() throws IOException {
		return readInt(baseStream);
	}

	public long readLong() throws IOException {
		byte[] buf = readBytes(8);

		return readLong(buf, 0);
	}

	@Override
	public int available() throws IOException {
		return baseStream.available();
	}

	@Override
	public void close() throws IOException {
		baseStream.close();
	}

	@Override
	public void mark(int readlimit) {
		baseStream.mark(readlimit);
	}

	@Override
	public boolean markSupported() {
		return baseStream.markSupported();
	}

	@Override
	public int read() throws IOException {
		return baseStream.read();
	}

	@Override
	public int read(byte[] b, int offset, int length) throws IOException {
		return baseStream.read(b, offset, length);
	}

	@Override
	public int read(byte[] b) throws IOException {
		// TODO Auto-generated method stub
		return super.read(b);
	}

	@Override
	public synchronized void reset() throws IOException {
		baseStream.reset();
	}

	@Override
	public long skip(long n) throws IOException {
		return baseStream.skip(n);
	}

	public static byte[] readBytes(InputStream is, int length) throws IOException {
		byte[] buf = new byte[length];

		int count = 0;
		while ( count < length ) {
			int read = is.read(buf, count, length - count);

			// Reached end
			if ( read == -1 ) {
				// Stop early
				byte[] early = new byte[count];
				System.arraycopy(buf, 0, early, 0, count);
				return early;
			}

			count += read;
		}

		return buf;
	}

	public byte[] readBytes(int length) throws IOException {
		return readBytes(baseStream, length);
	}

	public static int readUShort(InputStream is) throws IOException {
		byte[] buf = readBytes(is, 2);

		buf = padOut(buf, 2);

		return readUShort(buf, 0);
	}

	public int readUShort() throws IOException {
		return readUShort(baseStream);
	}

	/**
	 * Read an unsigned 16-bit value.
	 *
	 * @param buf
	 * @param offset
	 * @return
	 */
	public static int readUShort( byte[] buf, int offset ) {
		return (buf[offset + 0] & 0xFF) + ((buf[offset + 1] & 0xFF) << 8);
	}

	public static long readLong( byte buf[], int offset ) {
		return ((long)buf[offset + 0] & 0xFF) + (((long)buf[offset + 1] & 0xFF) << 8)
				+ (((long)buf[offset + 2] & 0xFF) << 16) + (((long)buf[offset + 3] & 0xFF) << 24)
				+ (((long)buf[offset + 4] & 0xFF) << 32) + (((long)buf[offset + 5] & 0xFF) << 40)
				+ (((long)buf[offset + 6] & 0xFF) << 48) + (((long)buf[offset + 7] & 0xFF) << 56);
	}

	public static long readUInt( byte buf[], int offset ) {
		return (readInt(buf, offset) & INT_TO_LONG_MASK);
	}

	public static int readInt(InputStream is) throws IOException {
		byte[] buf = readBytes(is, 4);

		buf = padOut(buf, 4);

		return readInt(buf, 0);
	}

	public static byte[] padOut(byte[] input, int length) {
		if (input == null || input.length < length) {
			byte[] output = new byte[4];

			if (input == null) {
				return output;
			}

			for (int i = 0; i < input.length; i++) {
				output[i] = input[i];
			}

			return output;
		}

		return input;
	}

	public static long readUInt(InputStream is) throws IOException {
		return (readInt(is) & INT_TO_LONG_MASK);
	}

	/**
	 * Read a 32-bit value.
	 *
	 * @param buf
	 * @param offset
	 * @return
	 */
	public static int readInt( byte buf[], int offset ) {
		return (buf[offset + 0] & 0xFF) + ((buf[offset + 1] & 0xFF) << 8) + ((buf[offset + 2] & 0xFF) << 16)
				+ ((buf[offset + 3] & 0xFF) << 24);
	}

	public UUID readUUID() throws IOException {
		byte[] buf = readBytes(16);

		return Types.bytestoUUID(buf);
	}

}
