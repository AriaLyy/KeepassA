/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.arialyy.frame.util.adapter;

import android.util.SparseArray;
import android.view.View;
import androidx.annotation.IdRes;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by lyy on 2015/12/3.
 * 通用Holder
 */
public class AbsHolder extends RecyclerView.ViewHolder {
  private View mView;
  private SparseArray<View> mViews = new SparseArray<>();

  public AbsHolder(View itemView) {
    super(itemView);
    mView = itemView;
  }

  @SuppressWarnings("unchecked")
  public <T extends View> T findViewById(@IdRes int id) {
    View view = mViews.get(id);
    if (view == null) {
      view = mView.findViewById(id);
      mViews.put(id, view);
    }
    return (T) view;
  }
}