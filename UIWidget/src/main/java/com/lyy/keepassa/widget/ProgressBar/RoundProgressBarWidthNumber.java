package com.lyy.keepassa.widget.ProgressBar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import com.example.uiwidget.R;

public class RoundProgressBarWidthNumber extends HorizontalProgressBarWithNumber {
  /**
   * mRadius of view
   */
  private int mRadius = dp2px(30);
  private int mMaxPaintWidth;
  private boolean isCountdown = false;

  public RoundProgressBarWidthNumber(Context context) {
    this(context, null);
  }

  public RoundProgressBarWidthNumber(Context context, AttributeSet attrs) {
    super(context, attrs);

    mReachedProgressBarHeight = (int) (mUnReachedProgressBarHeight * 2.5f);
    TypedArray ta = context.obtainStyledAttributes(attrs,
        R.styleable.RoundProgressBarWidthNumber);
    mRadius = (int) ta.getDimension(
        R.styleable.RoundProgressBarWidthNumber_radius, mRadius);
    isCountdown = ta.getBoolean(R.styleable.RoundProgressBarWidthNumber_progress_countdown, false);
    ta.recycle();

    mPaint.setStyle(Style.STROKE);
    mPaint.setAntiAlias(true);
    mPaint.setDither(true);
    mPaint.setStrokeCap(Cap.ROUND);
  }

  /**
   * 这里默认在布局中padding值要么不设置，要么全部设置
   */
  @Override
  protected synchronized void onMeasure(int widthMeasureSpec,
      int heightMeasureSpec) {

    mMaxPaintWidth = Math.max(mReachedProgressBarHeight,
        mUnReachedProgressBarHeight);
    int expect = mRadius * 2 + mMaxPaintWidth + getPaddingLeft()
        + getPaddingRight();
    int width = resolveSize(expect, widthMeasureSpec);
    int height = resolveSize(expect, heightMeasureSpec);
    int realWidth = Math.min(width, height);

    mRadius = (realWidth - getPaddingLeft() - getPaddingRight() - mMaxPaintWidth) / 2;

    setMeasuredDimension(realWidth, realWidth);
  }

  public void setCountdown(boolean countdown) {
    isCountdown = countdown;
  }

  @SuppressLint("DrawAllocation")
  @Override
  protected synchronized void onDraw(Canvas canvas) {
    String text = getProgress() + (showPercent ? "%" : "");
    float textWidth = mPaint.measureText(text);
    float textHeight = (mPaint.descent() + mPaint.ascent()) / 2;

    canvas.save();
    canvas.translate(getPaddingLeft() + (mMaxPaintWidth >> 1), getPaddingTop()
        + (mMaxPaintWidth >> 1));
    mPaint.setStyle(Style.STROKE);
    // draw unreaded bar
    mPaint.setColor(mUnReachedBarColor);
    mPaint.setStrokeWidth(mUnReachedProgressBarHeight);
    canvas.drawCircle(mRadius, mRadius, mRadius, mPaint);
    // draw reached bar
    mPaint.setColor(mReachedBarColor);
    mPaint.setStrokeWidth(mReachedProgressBarHeight);
    float sweepAngle = !isCountdown ? getProgress() * 1.0f / getMax() * 360
        : 360 - getProgress() * 1.0f / getMax() * 360;
    canvas.drawArc(new RectF(0, 0, mRadius << 1, mRadius << 1), 0,
        sweepAngle, false, mPaint);
    // draw text
    mPaint.setStyle(Style.FILL);
    canvas.drawText(text, mRadius - textWidth / 2, mRadius - textHeight,
        mPaint);

    canvas.restore();
  }
}