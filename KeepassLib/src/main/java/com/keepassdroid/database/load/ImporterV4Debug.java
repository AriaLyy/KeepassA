/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database.load;

import com.keepassdroid.database.PwDatabaseV4Debug;
import com.keepassdroid.database.exception.InvalidDBException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ImporterV4Debug extends ImporterV4 {

  public ImporterV4Debug(File streamDir) {
    super(streamDir);
  }

  @Override
  protected PwDatabaseV4Debug createDB() {
    return new PwDatabaseV4Debug();
  }

  @Override
  public PwDatabaseV4Debug openDatabase(InputStream inStream, String password,
      InputStream keyInputFile, long roundsFix) throws IOException,
      InvalidDBException {
    return (PwDatabaseV4Debug) super.openDatabase(inStream, password, keyInputFile, roundsFix);
  }
}