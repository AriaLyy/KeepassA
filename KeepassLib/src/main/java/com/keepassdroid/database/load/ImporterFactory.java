/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database.load;

import com.keepassdroid.database.PwDbHeaderV3;
import com.keepassdroid.database.PwDbHeaderV4;
import com.keepassdroid.database.exception.InvalidDBSignatureException;
import com.keepassdroid.stream.LEDataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ImporterFactory {
  public static Importer createImporter(InputStream is, File streamDir)
      throws InvalidDBSignatureException, IOException {
    return createImporter(is, streamDir, false);
  }

  public static Importer createImporter(InputStream is, File streamDir, boolean debug)
      throws InvalidDBSignatureException, IOException {
    int sig1 = LEDataInputStream.readInt(is);
    int sig2 = LEDataInputStream.readInt(is);

    if (PwDbHeaderV3.matchesHeader(sig1, sig2)) {
      if (debug) {
        return new ImporterV3Debug();
      }

      return new ImporterV3();
    } else if (PwDbHeaderV4.matchesHeader(sig1, sig2)) {
      return new ImporterV4(streamDir);
    }

    throw new InvalidDBSignatureException();
  }
}