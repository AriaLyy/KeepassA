package com.lyy.keepassa.widget.expand;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;
import com.example.uiwidget.R;

/**
 * Created by juan on 2018/06/26.
 */
public class ButtonSpan extends ClickableSpan {

  View.OnClickListener onClickListener;
  private Context context;
  private int colorId;

  public ButtonSpan(Context context, View.OnClickListener onClickListener) {
    this(context, onClickListener, R.color.white);
  }

  public ButtonSpan(Context context, View.OnClickListener onClickListener, int colorId) {
    this.onClickListener = onClickListener;
    this.context = context;
    this.colorId = colorId;
  }

  @Override
  public void updateDrawState(TextPaint ds) {
    ds.setColor(context.getResources().getColor(colorId));
    ds.setTextSize(dip2px(16));
    ds.setUnderlineText(false);
  }

  @Override
  public void onClick(View widget) {
    if (onClickListener != null) {
      onClickListener.onClick(widget);
    }
  }

  public static int dip2px(float dipValue) {
    final float scale = Resources.getSystem().getDisplayMetrics().density;
    return (int) (dipValue * scale + 0.5f);
  }
}
