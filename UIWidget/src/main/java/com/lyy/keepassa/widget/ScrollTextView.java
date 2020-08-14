package com.lyy.keepassa.widget;

import android.content.Context;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * 地址：https://www.cnblogs.com/lizhanqi/p/8520836.html
 * 可滑动的TextView, 并且解决了与 ScrollView等的滑动冲突
 */
public class ScrollTextView extends AppCompatTextView {
  public ScrollTextView(Context context) {
    super(context);
    setMovementMethod(ScrollingMovementMethod.getInstance());
  }

  public ScrollTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setMovementMethod(ScrollingMovementMethod.getInstance());
  }

  public ScrollTextView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setMovementMethod(ScrollingMovementMethod.getInstance());
  }

  float lastScrollY = 0;

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    if (getLineCount() > getMaxLines()) {
      if (ev.getAction() == MotionEvent.ACTION_DOWN) {
        lastScrollY = ev.getRawY();
        //Log.d("lldd", "down:" + lastScrollY);
      } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
        //滑动到头并且还在继续上滑动,或者滑动到底部就不要再拦截了(有误差)
        int sum = getLineHeight() * getLineCount() - getLineHeight() * getMaxLines();
        //计算上次与本次差
        float diff = lastScrollY - ev.getRawY();
        if (diff > 0) {//下滑动并且到达了底部也不要处理了
          //底部这里用abs的原因是,因为计算sum的时候有些误差
          if (Math.abs(sum - getScrollY()) < 5) {
            getParent().requestDisallowInterceptTouchEvent(false);
          } else {
            getParent().requestDisallowInterceptTouchEvent(true);
          }
        } else if (diff < 0) {//上滑动
          if (getScrollY() == 0) {//上滑动并且已经到达了顶部就不要在处理了
            getParent().requestDisallowInterceptTouchEvent(false);
          } else {
            getParent().requestDisallowInterceptTouchEvent(true);
          }
        }
        lastScrollY = ev.getRawY();
      } else {
        getParent().requestDisallowInterceptTouchEvent(false);
      }
    }
    return super.onTouchEvent(ev);
  }
}