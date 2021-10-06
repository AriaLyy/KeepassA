/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database.save;

import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwDatabaseV3;
import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwDbHeader;
import com.keepassdroid.database.exception.PwDbOutputException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public abstract class PwDbOutput {

  protected OutputStream mOS;

  public static PwDbOutput getInstance(PwDatabase pm, OutputStream os) {
    if (pm instanceof PwDatabaseV3) {
      return new PwDbV3Output((PwDatabaseV3) pm, os);
    } else if (pm instanceof PwDatabaseV4) {
      return new PwDbV4Output((PwDatabaseV4) pm, os);
    }

    return null;
  }

  protected PwDbOutput(OutputStream os) {
    mOS = os;
  }

  protected SecureRandom setIVs(PwDbHeader header) throws PwDbOutputException {
    SecureRandom random;
    try {
      random = SecureRandom.getInstance("SHA1PRNG");
    } catch (NoSuchAlgorithmException e) {
      throw new PwDbOutputException("Does not support secure random number generation.");
    }
    random.nextBytes(header.encryptionIV);
    random.nextBytes(header.masterSeed);

    return random;
  }

  public abstract void output() throws PwDbOutputException;

  public abstract PwDbHeader outputHeader(OutputStream os) throws PwDbOutputException;
}