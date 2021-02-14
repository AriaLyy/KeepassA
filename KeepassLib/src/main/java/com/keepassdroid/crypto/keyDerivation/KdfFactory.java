/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.crypto.keyDerivation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KdfFactory {

    public static List<KdfEngine> kdfList = new ArrayList<KdfEngine>();

    static {
        kdfList.add(new AesKdf());
        kdfList.add(new Argon2Kdf(Argon2Kdf.Argon2Type.D));
        kdfList.add(new Argon2Kdf(Argon2Kdf.Argon2Type.ID));
    }

    public static KdfParameters getDefaultParameters() {
        return kdfList.get(0).getDefaultParameters();
    }

    public static KdfEngine get(UUID uuid) {
        for (KdfEngine engine: kdfList) {
            if (engine.uuid.equals(uuid)) {
                return engine;
            }
        }

        return null;
    }

}