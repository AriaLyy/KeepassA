package com.lyy.keepassa.widget;

import android.text.Layout;
import android.text.NoCopySpan;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.widget.TextView;

public class OverLinkMovementMethod extends LinkMovementMethod {

  public static boolean canScroll = false;

  @Override
  public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
    int action = event.getAction();

    if (action == MotionEvent.ACTION_MOVE && !canScroll) {
      return true;
    }

    //// 防止事件传递给view
    //action = event.getActionMasked();
    //if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
    //
    //  int x = (int) event.getX();
    //  int y = (int) event.getY();
    //  x -= widget.getTotalPaddingLeft();
    //  y -= widget.getTotalPaddingTop();
    //  x += widget.getScrollX();
    //  y += widget.getScrollY();
    //  Layout layout = widget.getLayout();
    //  int line = layout.getLineForVertical(y);
    //  int off = layout.getOffsetForHorizontal(line, x);
    //
    //  ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);
    //  if (link.length > 0) {
    //    if (action == MotionEvent.ACTION_UP) {
    //      link[0].onClick(widget);
    //    } else {
    //      Selection.setSelection(buffer, buffer.getSpanStart(link[0]),
    //          buffer.getSpanEnd(link[0]));
    //    }
    //    return true;
    //  } else {
    //    Selection.removeSelection(buffer);
    //  }
    //}
    return super.onTouchEvent(widget, buffer, event);
  }

  public static MovementMethod getInstance() {
    if (sInstance == null) {
      sInstance = new OverLinkMovementMethod();
    }

    return sInstance;
  }

  private static OverLinkMovementMethod sInstance;
  private static Object FROM_BELOW = new NoCopySpan.Concrete();
}