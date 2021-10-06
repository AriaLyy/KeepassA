/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database;

import com.keepassdroid.database.iterator.EntrySearchStringIterator;
import java.util.Date;
import java.util.List;

public abstract class EntrySearchHandler extends EntryHandler<PwEntry> {
  private List<PwEntry> listStorage;
  private SearchParameters sp;
  private Date now;

  public static EntrySearchHandler getInstance(PwGroup group, SearchParameters sp,
      List<PwEntry> listStorage) {
    if (group instanceof PwGroupV3) {
      return new EntrySearchHandlerV4(sp, listStorage);
    } else if (group instanceof PwGroupV4) {
      return new EntrySearchHandlerV4(sp, listStorage);
    } else {
      throw new RuntimeException("Not implemented.");
    }
  }

  protected EntrySearchHandler(SearchParameters sp, List<PwEntry> listStorage) {
    this.sp = sp;
    this.listStorage = listStorage;
    now = new Date();
  }

  @Override
  public boolean operate(PwEntry entry) {
    if (sp.respectEntrySearchingDisabled && !entry.isSearchingEnabled()) {
      return true;
    }

    if (sp.excludeExpired && entry.expires() && now.after(entry.getExpiryTime())) {
      return true;
    }

    String term = sp.searchString;
    if (sp.ignoreCase) {
      term = term.toLowerCase();
    }

    if (searchStrings(entry, term)) {
      listStorage.add(entry);
      return true;
    }

    if (sp.searchInGroupNames) {
      PwGroup parent = entry.getParent();
      if (parent != null) {
        String groupName = parent.getName();
        if (groupName != null) {
          if (sp.ignoreCase) {
            groupName = groupName.toLowerCase();
          }

          if (groupName.indexOf(term) >= 0) {
            listStorage.add(entry);
            return true;
          }
        }
      }
    }

    if (searchID(entry)) {
      listStorage.add(entry);
      return true;
    }

    return true;
  }

  protected boolean searchID(PwEntry entry) {
    return false;
  }

  private boolean searchStrings(PwEntry entry, String term) {
    EntrySearchStringIterator iter = EntrySearchStringIterator.getInstance(entry, sp);
    while (iter.hasNext()) {
      String str = iter.next();
      if (str != null & str.length() > 0) {
        if (sp.ignoreCase) {
          str = str.toLowerCase();
        }

        if (str.indexOf(term) >= 0) {
          return true;
        }
      }
    }

    return false;
  }
}