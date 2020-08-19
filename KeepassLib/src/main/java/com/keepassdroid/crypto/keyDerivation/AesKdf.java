/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.crypto.keyDerivation;

import com.keepassdroid.crypto.CryptoUtil;
import com.keepassdroid.crypto.finalkey.FinalKey;
import com.keepassdroid.crypto.finalkey.FinalKeyFactory;
import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.utils.Types;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.UUID;

public class AesKdf extends KdfEngine {
    public static final UUID CIPHER_UUID = Types.bytestoUUID(
            new byte[]{(byte) 0xC9, (byte) 0xD9, (byte) 0xF3, (byte) 0x9A, (byte) 0x62, (byte) 0x8A, (byte) 0x44, (byte) 0x60,
                    (byte) 0xBF, (byte) 0x74, (byte) 0x0D, (byte) 0x08, (byte)0xC1, (byte) 0x8A, (byte) 0x4F, (byte) 0xEA
            });

    public static final String ParamRounds = "R";
    public static final String ParamSeed = "S";

    public AesKdf() {
        uuid = CIPHER_UUID;
    }

    @Override
    public KdfParameters getDefaultParameters() {
        KdfParameters p = super.getDefaultParameters();
        p.setUInt32(ParamRounds, PwDatabaseV4.DEFAULT_ROUNDS);

        return p;
    }

    @Override
    public byte[] transform(byte[] masterKey, KdfParameters p) throws IOException {
        long rounds = p.getUInt64(ParamRounds);
        byte[] seed = p.getByteArray(ParamSeed);

        if (masterKey.length != 32) {
            masterKey = CryptoUtil.hashSha256(masterKey);
        }

        if (seed.length != 32) {
            seed = CryptoUtil.hashSha256(seed);
        }

        FinalKey key = FinalKeyFactory.createFinalKey();
        return key.transformMasterKey(seed, masterKey, rounds);
    }

    @Override
    public void randomize(KdfParameters p) {
        SecureRandom random = new SecureRandom();

        byte[] seed = new byte[32];
        random.nextBytes(seed);

        p.setByteArray(ParamSeed, seed);
    }
}