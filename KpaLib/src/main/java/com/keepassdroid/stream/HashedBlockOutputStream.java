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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashedBlockOutputStream extends OutputStream {

	private final static int DEFAULT_BUFFER_SIZE = 1024 * 1024; 
	
	private LEDataOutputStream baseStream;
	private int bufferPos = 0;
	private byte[] buffer;
	private long bufferIndex = 0;
	
	public HashedBlockOutputStream(OutputStream os) {
		init(os, DEFAULT_BUFFER_SIZE);
	}
	
	public HashedBlockOutputStream(OutputStream os, int bufferSize) {
		if ( bufferSize <= 0 ) {
			bufferSize = DEFAULT_BUFFER_SIZE;
		}
		
		init(os, bufferSize);
	}
	
	private void init(OutputStream os, int bufferSize) {
		baseStream = new LEDataOutputStream(os);
		buffer = new byte[bufferSize];
		
	}

	@Override
	public void write(int oneByte) throws IOException {
		byte[] buf = new byte[1];
		buf[0] = (byte)oneByte;
		write(buf, 0, 1);
	}

	@Override
	public void close() throws IOException {
		if ( bufferPos != 0 ) {
			// Write remaining buffered amount
			WriteHashedBlock();
		}
		
		// Write terminating block
		WriteHashedBlock();
		
		flush();
		baseStream.close();
	}

	@Override
	public void flush() throws IOException {
		baseStream.flush();
	}

	@Override
	public void write(byte[] b, int offset, int count) throws IOException {
		while ( count > 0 ) {
			if ( bufferPos == buffer.length ) {
				WriteHashedBlock();
			}
			
			int copyLen = Math.min(buffer.length - bufferPos, count);
			
			System.arraycopy(b, offset, buffer, bufferPos, copyLen);
			
			offset += copyLen;
			bufferPos += copyLen;
			
			count -= copyLen;
		}
	}

	private void WriteHashedBlock() throws IOException {
		baseStream.writeUInt(bufferIndex);
		bufferIndex++;
		
		if ( bufferPos > 0 ) {
			MessageDigest md = null;
			try {
				md = MessageDigest.getInstance("SHA-256");
			} catch (NoSuchAlgorithmException e) {
				throw new IOException("SHA-256 not implemented here.");
			}
			
			byte[] hash;
			md.update(buffer, 0, bufferPos);
			hash = md.digest();
			/*
			if ( bufferPos == buffer.length) {
				hash = md.digest(buffer);
			} else {
				byte[] b = new byte[bufferPos];
				System.arraycopy(buffer, 0, b, 0, bufferPos);
				hash = md.digest(b);
			}
			*/
			
			baseStream.write(hash);

		} else {
			// Write 32-bits of zeros
			baseStream.writeLong(0L);
			baseStream.writeLong(0L);
			baseStream.writeLong(0L);
			baseStream.writeLong(0L);
		}
		
		baseStream.writeInt(bufferPos);
		
		if ( bufferPos > 0 ) {
			baseStream.write(buffer, 0, bufferPos);
		}
		
		bufferPos = 0;
		
	}

	@Override
	public void write(byte[] buffer) throws IOException {
		write(buffer, 0, buffer.length);
	}

}