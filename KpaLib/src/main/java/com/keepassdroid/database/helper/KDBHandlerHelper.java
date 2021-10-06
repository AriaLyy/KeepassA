/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database.helper;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import com.keepassdroid.Database;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwGroupV4;
import com.keepassdroid.database.PwIconCustom;
import com.keepassdroid.database.PwIconStandard;
import com.keepassdroid.database.exception.InvalidDBException;
import com.keepassdroid.database.exception.PwDbOutputException;
import com.keepassdroid.utils.UriUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KDBHandlerHelper {
  private static final String TAG = "KDBHandlerHelper";

  private static KDBHandlerHelper INSTANCE = null;
  private Context context;

  private KDBHandlerHelper(Context context) {
    this.context = context.getApplicationContext();
  }

  public synchronized static KDBHandlerHelper getInstance(Context context) {
    if (INSTANCE == null) {
      synchronized (KDBHandlerHelper.class) {
        INSTANCE = new KDBHandlerHelper(context);
      }
    }
    return INSTANCE;
  }

  /**
   * 打开数据库
   *
   * @param dbName 数据库名字
   * @param dbPath 数据库保存路径
   * @param pass 数据库密码
   * @param keyFilePath 密钥文件路径
   */
  public Database openDb(String dbName, String dbPath, String pass, String keyFilePath) {
    if (TextUtils.isEmpty(dbName)) {
      Log.e(TAG, "数据库名为空");
      return null;
    }
    if (TextUtils.isEmpty(dbPath)) {
      Log.e(TAG, "数据库路径为空");
      return null;
    }
    if (pass == null) {
      Log.e(TAG, "密码为空");
      return null;
    }
    // 创建db实体
    Database db = new Database();

    // Set Database state
    db.pm = PwDatabase.getNewDBInstance(dbName);
    db.mUri = UriUtil.parseDefaultFile(dbName);
    db.setLoaded();

    // 加载数据
    Uri dbUri = UriUtil.parseDefaultFile(dbPath);
    Uri keyFileUri = null;
    if (!TextUtils.isEmpty(keyFilePath)) {
      keyFileUri = UriUtil.parseDefaultFile(keyFilePath);
    }

    try {
      db.LoadData(context, dbUri, pass, keyFileUri);
    } catch (IOException | InvalidDBException e) {
      e.printStackTrace();
      return null;
    }
    return db;
  }

  /**
   * 打开数据库
   *
   * @param dbUri 数据库保存路径
   * @param pass 数据库密码
   * @param keyUri 密钥文件路径
   */
  public Database openDb(Uri dbUri, String pass, Uri keyUri)
      throws IOException, InvalidDBException {

    if (dbUri == null) {
      Log.e(TAG, "数据库路径为空");
      return null;
    }
    if (pass == null) {
      Log.e(TAG, "密码为空");
      return null;
    }

    Database db = new Database();
    db.LoadData(context, dbUri, pass, keyUri);
    return db;
  }

  /**
   * 删除条目
   *
   * @param save 是否需要保存数据库；true 删除后保存数据库，false 删除后不保存数据库
   */
  public void deleteEntry(Database db, PwEntry entry, boolean save) {
    PwDatabase pm = db.pm;
    PwGroup parent = entry.getParent();

    // Remove Entry from parent
    boolean recycle = pm.canRecycle(entry);
    if (recycle) {
      pm.recycle(entry);
    } else {
      pm.deleteEntry(entry);
    }

    if (!save) {
      return;
    }

    // 保存数据库
    if (save(db)) {
      // Mark parent dirty
      if (parent != null) {
        db.dirty.add(parent);
      }

      if (recycle) {
        PwGroup recycleBin = pm.getRecycleBin();
        db.dirty.add(recycleBin);
        db.dirty.add(pm.rootGroup);
      }
    } else {
      if (recycle) {
        pm.undoRecycle(entry, parent);
      } else {
        pm.undoDeleteEntry(entry, parent);
      }
    }
  }

  /**
   * 保存条目
   */
  public void saveEntry(Database db, PwEntry entry) {
    PwDatabase pm = db.pm;
    pm.addEntryTo(entry, entry.getParent());

    if (save(db)) {
      PwGroup parent = entry.getParent();
      // Mark parent group dirty
      db.dirty.add(parent);
    } else {
      pm.removeEntryFrom(entry, entry.getParent());
    }
  }

  /**
   * 更新条目
   *
   * @param newE 新的数据
   * @param oldE 旧的数据
   */
  public void updareEntry(Database db, PwEntry oldE, PwEntry newE) {
    PwEntry backup;
    backup = (PwEntry) oldE.clone();
    oldE.assign(newE);
    oldE.touch(true, true);

    if (save(db)) {
      // Mark group dirty if title or icon changes
      if (!backup.getTitle().equals(newE.getTitle()) || !backup.getIcon().equals(newE.getIcon())) {
        PwGroup parent = backup.getParent();
        if (parent != null) {
          // Resort entries
          parent.sortEntriesByName();

          // Mark parent group dirty
          db.dirty.add(parent);
        }
      }
    } else {
      oldE.assign(backup);
    }
  }

  /**
   * 保存分组
   *
   * @param name 组名
   * @param icon 分组图标
   * @param parent 父组，如果是想添加到跟目录，设置null
   */
  public PwGroup createGroup(Database db, String name, PwIconStandard icon, PwGroup parent) {
    PwDatabase pm = db.pm;

    // Generate new group
    PwGroup group = pm.createGroup();
    group.initNewGroup(name, pm.newGroupId());
    group.icon = icon;
    pm.addGroupTo(group, parent);

    if (save(db)) {
      db.dirty.add(parent);
    } else {
      pm.removeGroupFrom(group, parent);
    }
    return group;
  }

  /**
   * 保存分组
   *
   * @param name 组名
   * @param customIcon 分组图标
   * @param parent 父组，如果是想添加到跟目录，设置null
   */
  public PwGroup createGroup(Database db, String name, PwIconCustom customIcon, PwGroupV4 parent) {
    PwDatabase pm = db.pm;

    // Generate new group
    PwGroupV4 group = (PwGroupV4) pm.createGroup();
    group.initNewGroup(name, pm.newGroupId());
    group.icon = pm.iconFactory.getIcon(48); // 需要设置默认图标
    group.customIcon = customIcon;
    pm.addGroupTo(group, parent);

    if (save(db)) {
      db.dirty.add(parent);
    } else {
      pm.removeGroupFrom(group, parent);
    }
    return group;
  }

  /**
   * 删除分组
   *
   * @param save 是否需要保存数据库；true 删除后保存数据库，false 删除后不保存数据库
   */
  public void deleteGroup(Database db, PwGroup group, boolean save) {
    // Remove child entries
    List<PwEntry> childEnt = new ArrayList<>(group.childEntries);
    for (int i = 0; i < childEnt.size(); i++) {
      deleteEntry(db, childEnt.get(i), false);
    }

    // Remove child groups
    List<PwGroup> childGrp = new ArrayList<>(group.childGroups);
    for (int i = 0; i < childGrp.size(); i++) {
      deleteGroup(db, childGrp.get(i), false);
    }

    // Remove from parent
    PwGroup parent = group.getParent();
    if (parent != null) {
      parent.childGroups.remove(group);
    }

    // Remove from PwDatabaseV3
    db.pm.getGroups().remove(group);
    if (!save) {
      return;
    }

    // 保存数据库
    if (save(db)) {
      db.pm.groups.remove(group.getId());

      // Remove group from the dirty global (if it is present), not a big deal if this fails
      db.dirty.remove(group);

      // Mark parent dirty
      PwGroup gparent = group.getParent();
      if (gparent != null) {
        db.dirty.add(gparent);
      }
      db.dirty.add(db.pm.rootGroup);
    }
  }

  /**
   * 保存数据
   *
   * @return true 保存成功，false 保存失败
   */
  public synchronized boolean save(Database db) {

    try {
      db.SaveData(context);
      return true;
    } catch (IOException | PwDbOutputException e) {
      Log.e(TAG, "保存数据库失败", e);
    }
    return false;
  }
}