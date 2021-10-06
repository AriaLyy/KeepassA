/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database.load;

import com.keepassdroid.database.PwDatabaseV3Debug;
import com.keepassdroid.database.exception.InvalidDBException;
import java.io.IOException;
import java.io.InputStream;

public class ImporterV3Debug extends ImporterV3 {

  @Override
  protected PwDatabaseV3Debug createDB() {
    return new PwDatabaseV3Debug();
  }

  @Override
  public PwDatabaseV3Debug openDatabase(InputStream inStream, String password,
      InputStream keyInputStream, long roundsFix) throws IOException,
      InvalidDBException {
    return (PwDatabaseV3Debug) super.openDatabase(inStream, password, keyInputStream, roundsFix);
  }
}