/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database.load;

import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.exception.InvalidDBException;
import java.io.IOException;
import java.io.InputStream;

public abstract class Importer {

  public static final boolean DEBUG = true;

  public abstract PwDatabase openDatabase(InputStream inStream, String password,
      InputStream keyInputStream)
      throws IOException, InvalidDBException;

  public abstract PwDatabase openDatabase(InputStream inStream, String password,
      InputStream keyInputStream, long roundsFix)
      throws IOException, InvalidDBException;
}