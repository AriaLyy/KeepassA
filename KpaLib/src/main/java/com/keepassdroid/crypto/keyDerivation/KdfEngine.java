/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.crypto.keyDerivation;

import java.io.IOException;
import java.util.UUID;

public abstract class KdfEngine {
    public UUID uuid;

    public KdfParameters getDefaultParameters() {
        return new KdfParameters(uuid);
    }

    public abstract byte[] transform(byte[] masterKey, KdfParameters p) throws IOException;

    public abstract void randomize(KdfParameters p);

}