/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.dialog;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import com.airbnb.lottie.LottieAnimationView;
import com.arialyy.frame.core.AbsDialog;
import com.lyy.keepassa.R;
import com.lyy.keepassa.base.BaseApp;
import com.lyy.keepassa.databinding.DialogLoadingBinding;
import java.io.IOException;

/**
 * Created by AriaL on 2017/12/15.
 */
public class LoadingDialog extends AbsDialog<DialogLoadingBinding> {
  private LottieAnimationView animationView;

  public LoadingDialog(Context context) {
    super(context);
    if (getWindow() != null) {
      getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }
    animationView = findViewById(R.id.anim);
    setCancelable(false);
  }

  @Override protected void onStart() {
    super.onStart();
    try {
      animationView.setAnimation(
          getContext().getAssets().open("loadingAnimation.json", AssetManager.ACCESS_STREAMING),
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