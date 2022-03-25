/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.dialog;

import android.content.res.AssetManager;
import android.view.ViewGroup;
import androidx.fragment.app.DialogFragment;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.lyy.keepassa.R;
import com.lyy.keepassa.base.BaseApp;
import com.lyy.keepassa.base.BaseDialog;
import com.lyy.keepassa.databinding.DialogLoadingBinding;
import java.io.IOException;

/**
 * Created by AriaL on 2017/12/15.
 */
@Route(path = "/dialog/loading")
public class LoadingDialog extends BaseDialog<DialogLoadingBinding> {

  @Override protected void initData() {
    super.initData();
    setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    getDialog().getWindow()
        .setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

    setCancelable(false);
    try {
      getBinding().anim.setAnimation(
          requireContext().getAssets().open("loadingAnimation.json", AssetManager.ACCESS_STREAMING),
          "LottieCache");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected int setLayoutId() {
    return R.layout.dialog_loading;
  }

  public void dismiss(long delay) {
    BaseApp.handler.postDelayed(this::dismiss, delay);
  }
}