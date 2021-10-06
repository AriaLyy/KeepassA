/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.crypto.keyDerivation;
import com.keepassdroid.collections.VariantDictionary;
import com.keepassdroid.stream.LEDataInputStream;
import com.keepassdroid.stream.LEDataOutputStream;
import com.keepassdroid.utils.Types;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

public class KdfParameters extends VariantDictionary {

  public UUID kdfUUID;

  private static final String ParamUUID = "$UUID";

  public KdfParameters(UUID uuid) {
    kdfUUID = uuid;
    this.setByteArray(ParamUUID, Types.UUIDtoBytes(uuid));
  }

  public static KdfParameters deserialize(byte[] data) throws IOException {
    ByteArrayInputStream bis = new ByteArrayInputStream(data);
    LEDataInputStream lis = new LEDataInputStream(bis);

    VariantDictionary d = VariantDictionary.deserialize(lis);

    byte[] uuidBytes = d.getByteArray((ParamUUID));
    UUID uuid;
    if (uuidBytes != null) {
      uuid = Types.bytestoUUID(uuidBytes);
    } else {
      // Correct issue where prior versions were writing empty UUIDs for AES
      uuid = AesKdf.CIPHER_UUID;
    }

    KdfParameters kdfP = new KdfParameters(uuid);
    kdfP.copyTo(d);
    return kdfP;

  }

  public static byte[] serialize(KdfParameters kdf) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    LEDataOutputStream los = new LEDataOutputStream(bos);

    KdfParameters.serialize(kdf, los);

    return bos.toByteArray();
  }

}