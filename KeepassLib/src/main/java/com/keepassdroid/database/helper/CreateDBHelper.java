/*
 * Copyright 2009-2016 Brian Pellin.
 *
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.database.helper;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import com.keepassdroid.Database;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.exception.InvalidKeyFileException;
import com.keepassdroid.utils.UriUtil;
import java.io.IOException;
import java.io.InputStream;

/**
 * 创建数据库
 */
public class CreateDBHelper {
  private static final String TAG = "CreateDBHelper";
  private final int DEFAULT_ENCRYPTION_ROUNDS = 300;
  private Database db;
  private Context context;
  private int encryptionRound = DEFAULT_ENCRYPTION_ROUNDS;
  private Uri keyFile;
  private String pass;

  /**
   * 密码验证回调
   */
  public interface ValidateCallback {
    /**
     * @param success true 验证通过，验证不通过吃需要弹出警告
     */
    // TODO: 2020-02-04 警告内容 The .kdb format only supports the Latin1 character set. Your password may contain characters outside of this character set. All non-Latin1 charaters are converted to the same character, which reduces the security of your password. Changing your password is recommended.
    void callback(boolean success);
  }

  /**
   * @param dbName 数据库名
   * @param dbUri 数据库的uri，file uri 或 content uri
   */
  public CreateDBHelper(Context context, String dbName, Uri dbUri) {
    this.context = context.getApplicationContext();
    db = new Database();

    PwDatabase pm = PwDatabase.getNewDBInstance(dbName);
    pm.initNew(dbName);

    // Set Database state
    db.pm = pm;
    db.mUri = UriUtil.parseDefaultFile(dbUri);
    db.setLoaded();
  }

  /**
   * 设置数据库加密次数
   *
   * @param rounds 数据库加密次数
   * @deprecated 这个有问题，设置了加密次数就无法正常打开了
   */
  @Deprecated
  public CreateDBHelper setEncryptionRounds(int rounds) {
    encryptionRound = rounds;
    return this;
  }

  /**
   * 设置密钥文件
   *
   * @param keyFileUri 密钥文件路径
   */
  public CreateDBHelper setKeyFile(Uri keyFileUri) {
    if (keyFileUri == null) {
      Log.e(TAG, "密钥文件路径为空");
    } else {
      keyFile = keyFileUri;
    }
    return this;
  }

  /**
   * 设置数据库密码
   *
   * @param pass 数据库密码
   * @param callback 密码验证回调，v3版本的数据默认编码是"ISO-8859-1"，所以需要弹出警告
   */
  public CreateDBHelper setPass(String pass, ValidateCallback callback) {
    if (TextUtils.isEmpty(pass)) {
      throw new NullPointerException("密码为空");
    }
    if (callback != null) {
      if (db.pm.validatePasswordEncoding(pass)) {
        callback.callback(true);
      } else {
        callback.callback(false);
      }
    }

    this.pass = pass;
    return this;
  }

  /**
   * 构建数据库
   */
  public Database build() {
    PwDatabase pm = db.pm;

    byte[] backupKey = new byte[pm.masterKey.length];
    System.arraycopy(pm.masterKey, 0, backupKey, 0, backupKey.length);

    // Set key
    try {
      InputStream is = UriUtil.getUriInputStream(context, keyFile);
      pm.setMasterKey(pass, is);
    } catch (InvalidKeyFileException e) {
      erase(backupKey);
      e.printStackTrace();
      return null;
    } catch (IOException e) {
      erase(backupKey);
      e.printStackTrace();
      return null;
    }
    //pm.setNumRounds(encryptionRound);

    try {
      db.SaveData(context);
    } catch (Exception e) {
      e.printStackTrace();
      erase(pm.masterKey);
      pm.masterKey = backupKey;
    }
    return db;
  }

  /**
   * Overwrite the array as soon as we don't need it to avoid keeping the extra data in memory
   */
  private void erase(byte[] array) {
    if (array == null) return;

    for (int i = 0; i < array.length; i++) {
      array[i] = 0;
    }
  }
}
