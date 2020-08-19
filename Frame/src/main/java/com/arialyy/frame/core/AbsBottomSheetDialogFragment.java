/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.arialyy.frame.core;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import com.arialyy.frame.util.StringUtil;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.lyy.frame.R;

public abstract class AbsBottomSheetDialogFragment<VB extends ViewDataBinding>
    extends BottomSheetDialogFragment {

  protected String TAG = StringUtil.getClassName(this);
  private VB mBind;
  protected View mRootView;
  private OnDismissListener onDismissListener;

  public interface OnDismissListener {
    void onDismiss(BottomSheetDialogFragment dialog);
  }

  public AbsBottomSheetDialogFragment(){

  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);
  }

  @Override public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    mBind = DataBindingUtil.inflate(inflater, setLayoutId(), container, false);
    mRootView = mBind.getRoot();
    return mRootView;
  }

  @Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    init(savedInstanceState);
  }

  public void setOnDismissListener(OnDismissListener onDismissListener) {
    this.onDismissListener = onDismissListener;
  }

  protected abstract void init(Bundle savedInstanceState);

  /**
   * 设置资源布局
   */
  protected abstract int setLayoutId();

  @Override public void onDismiss(@NonNull DialogInterface dialog) {
    super.onDismiss(dialog);
    if (onDismissListener != null){
      onDismissListener.onDismiss(this);
    }
  }

  /**
   * 获取binding对象
   */
  protected VB getBinding() {
    return mBind;
  }

  @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    PermissionHelp.getInstance().handlePermissionCallback(requestCode, permissions, grantResults);
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    PermissionHelp.getInstance()
        .handleSpecialPermissionCallback(getContext(), requestCode, resultCode, data);
  }
}