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

import javax.crypto.Mac;

public class MacOutputStream extends OutputStream {
    private Mac mac;
    private OutputStream os;

    public MacOutputStream(OutputStream os, Mac mac) {
        this.mac = mac;
        this.os = os;
    }

    @Override
    public void flush() throws IOException {
        os.flush();
    }

    @Override
    public void close() throws IOException {
        os.close();
    }

    @Override
    public void write(int oneByte) throws IOException {
        mac.update((byte) oneByte);
        os.write(oneByte);
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        mac.update(buffer, offset, count);
        os.write(buffer, offset, count);
    }

    @Override
    public void write(byte[] buffer) throws IOException {
        mac.update(buffer, 0, buffer.length);
        os.write(buffer);
    }

    public byte[] getMac() {
        return mac.doFinal();
    }
}