/*
 * Copyright 2009-2017 Brian Pellin.
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
package com.keepassdroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwDatabaseV3;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.exception.InvalidDBException;
import com.keepassdroid.database.exception.InvalidPasswordException;
import com.keepassdroid.database.exception.PwDbOutputException;
import com.keepassdroid.database.load.Importer;
import com.keepassdroid.database.load.ImporterFactory;
import com.keepassdroid.database.save.PwDbOutput;
import com.keepassdroid.utils.UriUtil;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SyncFailedException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashSet;
import java.util.Set;
import keepass2android.kp2akeytransform.R;
import org.apache.commons.io.FileUtils;

/**
 * @author bpellin
 */
public class Database {

  private static final String TAG = Database.class.getName();

  public Set<PwGroup> dirty = new HashSet<PwGroup>();
  public PwDatabase pm;
  // 数据库uri
  public Uri mUri;
  public boolean readOnly = false;
  public boolean passwordEncodingError = false;
  private String cacheDbName = null;
  private File cacheDb;

  private boolean loaded = false;

  public boolean Loaded() {
    return loaded;
  }

  public void setLoaded() {
    loaded = true;
  }

  /**
   * @param dbUri 数据库的uri
   * @param password 数据库密码
   * @param keyUri 密钥的uri
   * @throws IOException
   * @throws InvalidDBException
   */
  public void LoadData(Context ctx, Uri dbUri, String password, Uri keyUri)
      throws IOException, InvalidDBException {
    LoadData(ctx, dbUri, password, keyUri, !Importer.DEBUG);
  }

  private void LoadData(Context ctx, Uri dbUri, String password, Uri keyUri, boolean debug)
      throws IOException, InvalidDBException {
    mUri = dbUri;
    readOnly = false;
    if (dbUri.getScheme().equals("file")) {
      File file = new File(dbUri.getPath());
      readOnly = !file.canWrite();
    }

    try {
      LoadData(ctx, dbUri, password, keyUri, debug, 0);
    } catch (InvalidPasswordException e) {
      // Retry with rounds fix
      try {
        LoadData(ctx, dbUri, password, keyUri, debug, getFixRounds(ctx));
      } catch (Exception e2) {
        // Rethrow original exception
        throw e;
      }
    }
  }

  /**
   * 获取加密次数
   */
  private long getFixRounds(Context ctx) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    return prefs.getLong(ctx.getString(R.string.roundsFix_key),
        ctx.getResources().getInteger(R.integer.roundsFix_default));
  }

  /**
   * 加载并验证数据
   *
   * @param dbUri 数据库的uri
   * @param password 数据库密码
   * @param keyUri 密钥uri
   * @param roundsFix 加密次数
   * @throws IOException
   * @throws InvalidDBException
   */
  private void LoadData(Context ctx, Uri dbUri, String password, Uri keyUri,
      boolean debug, long roundsFix) throws IOException, InvalidDBException {
    if (cacheDb == null) {
      cacheDbName = UriUtil.getFileNameFromUri(ctx, mUri);
      cacheDb = new File(ctx.getFilesDir(), cacheDbName == null ? "cache.kdbx" : cacheDbName);
    }

    BufferedInputStream bis = new BufferedInputStream(UriUtil.getUriInputStream(ctx, dbUri));
    if (!bis.markSupported()) {
      throw new IOException("Input stream does not support mark.");
    }

    InputStream kis = UriUtil.getUriInputStream(ctx, keyUri);

    // We'll end up reading 8 bytes to identify the header. Might as well use two extra.
    bis.mark(10);

    try {
      Importer imp = ImporterFactory.createImporter(bis, ctx.getFilesDir(), debug);

      bis.reset();  // Return to the start

      pm = imp.openDatabase(bis, password, kis, roundsFix);
    } catch (InvalidDBException ex) {
      bis.close();
      // 如果验证失败，尝试从缓存中读数据
      if (!cacheDb.exists()) {
        throw ex;
      }
      FileInputStream fis = new FileInputStream(cacheDb);
      Importer imp = ImporterFactory.createImporter(fis, ctx.getFilesDir(), debug);
      pm = imp.openDatabase(bis, password, kis, roundsFix);
      if (pm != null) {
        Log.d(TAG, "从缓存中恢复数据");
      }
      // 如果缓存数据验证成功，替换数据库为缓存数据库
      WritableByteChannel foc =
          Channels.newChannel(ctx.getContentResolver().openOutputStream(dbUri));
      FileChannel fic = new FileInputStream(cacheDb).getChannel();
      fic.transferTo(0, fic.size(), foc);
      fic.close();
      foc.close();
    }
    if (pm != null) {
      PwGroup root = pm.rootGroup;
      pm.populateGlobals(root);
      if (pm != null) {
        passwordEncodingError = !pm.validatePasswordEncoding(password);
      }
      loaded = true;
    }
    loaded = true;
  }

  public synchronized void SaveData(Context ctx) throws IOException, PwDbOutputException {
    SaveData(ctx, mUri);
  }

  /**
   * 原来的代码如果uri不是file时很可能会出现数据库损坏的情况
   * 1、先将需要保存的数据缓存下载了
   * 2、缓存成功后，再将原来的数据库替换为缓存的数据库
   */
  public synchronized void SaveData(Context ctx, Uri uri) throws IOException, PwDbOutputException {

    if (uri.getScheme().equals("file")) {
      File targetFile = new File(uri.getPath());
      File tempFile = new File(targetFile.getParentFile(), targetFile.getName() + ".tmp");
      if (!tempFile.getParentFile().exists()){
        tempFile.getParentFile().mkdirs();
      }
      FileOutputStream fos = new FileOutputStream(tempFile);

      PwDbOutput pmo = PwDbOutput.getInstance(pm, fos);
      pmo.output();
      fos.close();

      // Force data to disk before continuing
      try {
        fos.getFD().sync();
      } catch (SyncFailedException e) {
        // Ignore if fsync fails. We tried.
      }


      if (!tempFile.renameTo(targetFile)) {
        throw new IOException("Failed to store database.");
      }
    } else {
      // 1.将数据存到缓存中
      if (cacheDb == null) {
        cacheDbName = UriUtil.getFileNameFromUri(ctx, mUri);
        cacheDb = new File(ctx.getFilesDir(), cacheDbName == null ? "cache.kdbx" : cacheDbName);
      }
      if (cacheDb.exists()) {
        cacheDb.delete();
      }
      cacheDb.createNewFile();

      // 2.写入数据库数据
      OutputStream os = new FileOutputStream(cacheDb);
      PwDbOutput pmo = PwDbOutput.getInstance(pm, os);
      pmo.output();
      os.close();

      // 3.将缓存数据替换原数据库
      WritableByteChannel foc = Channels.newChannel(ctx.getContentResolver().openOutputStream(uri));
      FileChannel fic = new FileInputStream(cacheDb).getChannel();
      fic.transferTo(0, fic.size(), foc);
      fic.close();
      foc.close();
    }
    mUri = uri;
  }

  public void clear(Context context) {
    dirty.clear();
    //drawFactory.clear();
    // Delete the cache of the database if present
    if (pm != null) {
      pm.clearCache();
    }
    // In all cases, delete all the files in the temp dir
    try {
      FileUtils.cleanDirectory(context.getFilesDir());
    } catch (IOException e) {
      Log.e(TAG, "Unable to clear the directory cache.", e);
    }

    pm = null;
    mUri = null;
    loaded = false;
    passwordEncodingError = false;
  }

  public void markAllGroupsAsDirty() {
    for (PwGroup group : pm.getGroups()) {
      dirty.add(group);
    }

    // TODO: This should probably be abstracted out
    // The root group in v3 is not an 'official' group
    if (pm instanceof PwDatabaseV3) {
      dirty.add(pm.rootGroup);
    }
  }
}
