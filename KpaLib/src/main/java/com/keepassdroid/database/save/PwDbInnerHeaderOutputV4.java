/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database.save;

import android.util.Log;
import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwDbHeaderV4;
import com.keepassdroid.database.PwDbHeaderV4.KdbxBinaryFlags;
import com.keepassdroid.database.PwDbHeaderV4.PwDbInnerHeaderV4Fields;
import com.keepassdroid.database.security.ProtectedBinary;
import com.keepassdroid.stream.LEDataOutputStream;
import com.keepassdroid.utils.Util;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PwDbInnerHeaderOutputV4 {
  private String TAG = "PwDbInnerHeaderOutputV4";
  private PwDatabaseV4 db;
  private PwDbHeaderV4 header;
  private LEDataOutputStream los;

  public PwDbInnerHeaderOutputV4(PwDatabaseV4 db, PwDbHeaderV4 header, OutputStream os) {
    this.db = db;
    this.header = header;

    this.los = new LEDataOutputStream(os);
  }

  public void output() throws IOException {
    los.write(PwDbInnerHeaderV4Fields.InnerRandomStreamID);
    los.writeInt(4);
    los.writeInt(header.innerRandomStream.id);

    int streamKeySize = header.innerRandomStreamKey.length;
    los.write(PwDbInnerHeaderV4Fields.InnerRandomstreamKey);
    los.writeInt(streamKeySize);
    los.write(header.innerRandomStreamKey);

    for (ProtectedBinary bin : db.binPool.binaries()) {
      InputStream inputStream = bin.getData();
      if (inputStream == null){
        Log.e(TAG, "附件信息为空");
        continue;
      }
      byte flag = KdbxBinaryFlags.None;
      if (bin.isProtected()) {
        flag |= KdbxBinaryFlags.Protected;
      }

      los.write(PwDbInnerHeaderV4Fields.Binary);
      los.writeInt(bin.length() + 1);
      los.write(flag);


      Util.copyStream(inputStream, los);
    }

    los.write(PwDbInnerHeaderV4Fields.EndOfHeader);
    los.writeInt(0);
  }
}