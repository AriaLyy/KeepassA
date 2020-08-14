package com.lyy.keepassa.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.appcompat.widget.AppCompatEditText;

/**
 * Created by bruce on 2017/10/4.
 */

public class MyEditText extends AppCompatEditText {

  private long lastTime = 0;

  public MyEditText(Context context) {
    this(context, null);
  }

  public MyEditText(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onSelectionChanged(int selStart, int selEnd) {
    super.onSelectionChanged(selStart, selEnd);
    if (getText() != null){
      this.setSelection(this.getText().length());
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      long currentTime = System.currentTimeMillis();
      if (currentTime - lastTime < 500) {
        lastTime = currentTime;
        return true;
      } else {
        lastTime = currentTime;
      }
    }
    return super.onTouchEvent(event);
  }
}