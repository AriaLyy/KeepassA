/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database;

import android.util.Log;
import com.keepassdroid.database.security.ProtectedBinary;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class BinaryPool {
  private HashMap<Integer, ProtectedBinary> pool = new HashMap<Integer, ProtectedBinary>();

  public BinaryPool() {

  }

  public BinaryPool(PwGroupV4 rootGroup) {
    build(rootGroup);
  }

  public ProtectedBinary get(int key) {
    return pool.get(key);
  }

  public ProtectedBinary put(int key, ProtectedBinary value) {
    return pool.put(key, value);
  }

  public Set<Entry<Integer, ProtectedBinary>> entrySet() {
    return pool.entrySet();
  }

  public void clear() {
    for (Entry<Integer, ProtectedBinary> entry : pool.entrySet())
      entry.getValue().clear();
    pool.clear();
  }

  public Collection<ProtectedBinary> binaries() {
    return pool.values();
  }

  private class AddBinaries extends EntryHandler<PwEntryV4> {

    @Override
    public boolean operate(PwEntryV4 entry) {
      for (PwEntryV4 histEntry : entry.history) {
        poolAdd(histEntry.binaries);
      }

      poolAdd(entry.binaries);
      return true;
    }
  }

  private void poolAdd(Map<String, ProtectedBinary> dict) {
    for (ProtectedBinary pb : dict.values()) {
      poolAdd(pb);
    }
  }

  public void poolAdd(ProtectedBinary pb) {
    assert (pb != null);

    if (poolFind(pb) != -1) return;

    pool.put(pool.size(), pb);
  }

  public int findUnusedKey() {
    int unusedKey = pool.size();
		while (get(unusedKey) != null) {
			unusedKey++;
		}
    return unusedKey;
  }

  public int poolFind(ProtectedBinary pb) {
    for (Entry<Integer, ProtectedBinary> pair : pool.entrySet()) {
      if (pair.getValue() == null) {
        Log.e("BinaryPool", "文件内容byte为空");
        continue;
      }
      if (pair.getValue().equals(pb)) {
        return pair.getKey();
      }
    }

    return -1;
  }

  private void build(PwGroupV4 rootGroup) {
    EntryHandler eh = new AddBinaries();
    rootGroup.preOrderTraverseTree(null, eh);
  }
}