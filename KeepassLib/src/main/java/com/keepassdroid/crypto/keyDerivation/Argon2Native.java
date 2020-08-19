/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.crypto.keyDerivation;

import com.keepassdroid.crypto.NativeLib;

import java.io.IOException;

public class Argon2Native {

    public static byte[] transformKey(byte[] password, byte[] salt, int parallelism,
                                              long memory, long iterations, byte[] secretKey,
                                              byte[] associatedData, long version) throws IOException {
        NativeLib.init();

        return nTransformMasterKey(password, salt, parallelism, memory, iterations, secretKey, associatedData, version);
    }

    private static native byte[] nTransformMasterKey(byte[] password, byte[] salt, int parallelism,
                                              long memory, long iterations, byte[] secretKey,
                                              byte[] associatedData, long version) throws IOException;
}