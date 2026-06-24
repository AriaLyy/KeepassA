/*
 *   Copyright 2014 Oguz Bilgener
 */
package com.lyy.keepassa.widgets.arc;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import com.lyy.keepassa.R;
import java.util.Objects;

/**
 * A simple button implementation with a similar look an feel to.
 */
public class SubActionButton extends FrameLayout {

  public static final int THEME_LIGHT = 0;
  public static final int THEME_DARK = 1;
  public static final int THEME_LIGHTER = 2;
  public static final int THEME_DARKER = 3;

  public SubActionButton(Context context, LayoutParams layoutParams, int theme,
      Drawable backgroundDrawable, View contentView, LayoutParams contentParams) {
    super(context);
    setLayoutParams(layoutParams);
    // If no custom backgroundDrawable is specified, use the background drawable of the theme.
    if (backgroundDrawable == null) {
      //if(theme == THEME_LIGHT) {
      //    backgroundDrawable = context.getResources().getDrawable(R.drawable.button_sub_action_selector);
      //}
      //else if(theme == THEME_DARK) {
      //    backgroundDrawable = context.getResources().getDrawable(R.drawable.button_sub_action_dark_selector);
      //}
      //else if(theme == THEME_LIGHTER) {
      //    backgroundDrawable = context.getResources().getDrawable(R.drawable.button_action_selector);
      //}
      //else if(theme == THEME_DARKER) {
      //    backgroundDrawable = context.getResources().getDrawable(R.drawable.button_action_dark_selector);
      //}
      //else {
      //    throw new RuntimeException("Unknown SubActionButton theme: " + theme);
      //}
    } else {
      backgroundDrawable = Objects.requireNonNull(
          backgroundDrawable.mutate().getConstantState()).newDrawable();
    }
    setBackgroundResource(backgroundDrawable);
    if (contentView != null) {
      setContentView(contentView, contentParams);
    }
    setClickable(true);
  }

  /**
   * Sets a content view with custom LayoutParams that will be displayed inside this
   * SubActionButton.
   */
  public void setContentView(View contentView, LayoutParams params) {
    if (params == null) {
      params =
          new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
      final int margin =
          getResources().getDimensionPixelSize(R.dimen.sub_action_button_content_margin);
      params.setMargins(margin, margin, margin, margin);
    }

    contentView.setClickable(false);
    this.addView(contentView, params);
  }

  /**
   * Sets a content view with default LayoutParams
   */
  public void setContentView(View contentView) {
    setContentView(contentView, null);
  }

  private void setBackgroundResource(Drawable drawable) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      setBackground(drawable);
    } else {
      setBackgroundDrawable(drawable);
    }
  }

  /**
   * A builder for {@link SubActionButton} in conventional Java Builder format
   */
  public static class Builder {

    private Context context;
    private LayoutParams layoutParams;
    private int theme;
    private Drawable backgroundDrawable;
    private View contentView;
    private LayoutParams contentParams;

    public Builder(Context context) {
      this.context = context;

      // Default SubActionButton settings
      int size = context.getResources().getDimensionPixelSize(R.dimen.sub_action_button_size);
      LayoutParams params = new LayoutParams(size, size, Gravity.TOP | Gravity.LEFT);
      setLayoutParams(params);
      setTheme(SubActionButton.THEME_LIGHT);
    }

    public Builder setLayoutParams(LayoutParams params) {
      this.layoutParams = params;
      return this;
    }

    public Builder setTheme(int theme) {
      this.theme = theme;
      return this;
    }

    public Builder setBackgroundDrawable(Drawable backgroundDrawable) {
      this.backgroundDrawable = backgroundDrawable;
      return this;
    }

    public Builder setContentView(View contentView) {
      this.contentView = contentView;
      return this;
    }

    public Builder setContentView(View contentView, LayoutParams contentParams) {
      this.contentView = contentView;
      this.contentParams = contentParams;
      return this;
    }

    public SubActionButton build() {
      return new SubActionButton(context,
          layoutParams,
          theme,
          backgroundDrawable,
          contentView,
          contentParams);
    }
  }
}
