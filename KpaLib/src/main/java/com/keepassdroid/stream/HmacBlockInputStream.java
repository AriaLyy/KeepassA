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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HmacBlockInputStream extends InputStream {
    private LEDataInputStream baseStream;
    private boolean verify;
    private byte[] key;
    private byte[] buffer;
    private int bufferPos = 0;
    private long blockIndex = 0;
    private boolean endOfStream = false;

    public HmacBlockInputStream(InputStream baseStream, boolean verify, byte[] key) {
        super();

        this.baseStream = new LEDataInputStream(baseStream);
        this.verify = verify;
        this.key = key;
        buffer = new byte[0];
    }

    @Override
    public int read() throws IOException {
        if (endOfStream) return -1;

        if (bufferPos == buffer.length) {
            if (!readSafeBlock()) return -1;
        }

        int output = Types.readUByte(buffer, bufferPos);
        bufferPos++;

        return output;
    }

    @Override
    public int read(byte[] outBuffer, int byteOffset, int byteCount) throws IOException {
        int remaining = byteCount;
        while (remaining > 0) {
            if (bufferPos == buffer.length) {
                if (!readSafeBlock()) {
                    int read = byteCount - remaining;
                    if (read <= 0) {
                        return -1;
                    } else {
                        return byteCount - remaining;
                    }
                }
            }

            int copy = Math.min(buffer.length - bufferPos, remaining);
            assert(copy > 0);

            System.arraycopy(buffer, bufferPos, outBuffer, byteOffset, copy);
            byteOffset += copy;
            bufferPos += copy;

            remaining -= copy;
        }

        return byteCount;
    }

    @Override
    public int read(byte[] outBuffer) throws IOException {
        return read(outBuffer, 0, outBuffer.length);
    }

    private boolean readSafeBlock() throws IOException {
        if (endOfStream) return false;

        byte[] storedHmac = baseStream.readBytes(32);
        if (storedHmac == null || storedHmac.length != 32) {
            throw new IOException("File corrupted");
        }

        byte[] pbBlockIndex = LEDataOutputStream.writeLongBuf(blockIndex);
        byte[] pbBlockSize = baseStream.readBytes(4);
        if (pbBlockSize == null || pbBlockSize.length != 4) {
            throw new IOException("File corrupted");
        }
        int blockSize = LEDataInputStream.readInt(pbBlockSize, 0);
        bufferPos = 0;

        buffer = baseStream.readBytes(blockSize);

        if (verify) {
            byte[] cmpHmac;
            byte[] blockKey = HmacBlockStream.GetHmacKey64(key, blockIndex);
            Mac hmac;
            try {
                hmac = Mac.getInstance("HmacSHA256");
                SecretKeySpec signingKey = new SecretKeySpec(blockKey, "HmacSHA256");
                hmac.init(signingKey);
            } catch (NoSuchAlgorithmException e) {
                throw new IOException("Invalid Hmac");
            } catch (InvalidKeyException e) {
                throw new IOException("Invalid Hmac");
            }

            hmac.update(pbBlockIndex);
            hmac.update(pbBlockSize);

            if (buffer.length > 0) {
                hmac.update(buffer);
            }

            cmpHmac = hmac.doFinal();
            Arrays.fill(blockKey, (byte)0);

            if (!Arrays.equals(cmpHmac, storedHmac)) {
                throw new IOException("Invalid Hmac");
            }

        }

        blockIndex++;

        if (blockSize == 0) {
            endOfStream = true;
            return false;
        }

        return true;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void close() throws IOException {
        baseStream.close();
    }

    @Override
    public long skip(long byteCount) throws IOException {
        return 0;
    }

    @Override
    public int available() throws IOException {
        return buffer.length - bufferPos;
    }
}