/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.arialyy.frame.util.adapter;

import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.lyy.frame.R;

/*
 * RecyclerView item 事件监听帮助类
 * RvItemClickSupport.addTo(recyclerView).setOnItemClickListener(new RvItemClickSupport.OnItemClickListener() {
 *
 * @Override
 * public void onItemClicked(RecyclerView recyclerView, int position, View v) {
 *      //处理你的事件
 * });
 */
public class RvItemClickSupport {
  private final RecyclerView mRecyclerView;
  private OnItemClickListener mOnItemClickListener;
  private OnItemLongClickListener mOnItemLongClickListener;
  private OnItemTouchListener mOnItemTouchListener;
  private OnItemFocusChangeListener mOnItemFocusChangeListener;
  private OnItemKeyListener mOnItemKeyListener;

  public interface OnItemClickListener {
    void onItemClicked(RecyclerView recyclerView, int position, View v);
  }

  public interface OnItemLongClickListener {
    boolean onItemLongClicked(RecyclerView recyclerView, int position, View v);
  }

  public interface OnItemTouchListener {
    public void onTouchEvent(RecyclerView rv, MotionEvent e, int position, View v);
  }

  public interface OnItemFocusChangeListener {
    public void onFocusChange(View v, int position, boolean hasFocus);
  }

  public interface OnItemKeyListener {
    public boolean onKey(View v, int keyCode, int position, KeyEvent event);
  }

  private View.OnFocusChangeListener mOnFocusChangeListener = new View.OnFocusChangeListener() {

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
      if (mOnItemFocusChangeListener != null) {
        RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(v);
        mOnItemFocusChangeListener.onFocusChange(v, holder.getAdapterPosition(),
            holder.itemView.hasFocus());
      }
    }
  };

  private View.OnClickListener mOnClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      if (mOnItemClickListener != null) {
        RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(v);
        mOnItemClickListener.onItemClicked(mRecyclerView, holder.getAdapterPosition(), v);
      }
    }
  };

  private View.OnLongClickListener mOnLongClickListener = new View.OnLongClickListener() {
    @Override
    public boolean onLongClick(View v) {
      if (mOnItemLongClickListener != null) {
        RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(v);
        return mOnItemLongClickListener.onItemLongClicked(mRecyclerView,
            holder.getAdapterPosition(), v);
      }
      return false;
    }
  };

  private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
    @Override
    public boolean onTouch(View v, MotionEvent event) {
      if (mOnItemTouchListener != null) {
        RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(v);
        mOnItemTouchListener.onTouchEvent(mRecyclerView, event, holder.getAdapterPosition(), v);
      }
      return false;
    }
  };

  private View.OnKeyListener mOnKeyListener = new View.OnKeyListener() {
    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
      if (mOnItemKeyListener != null) {
        RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(v);
        return mOnItemKeyListener.onKey(v, keyCode, holder.getAdapterPosition(), event);
      }
      return false;
    }
  };

  private RecyclerView.OnChildAttachStateChangeListener mAttachListener =
      new RecyclerView.OnChildAttachStateChangeListener() {
        @Override
        public void onChildViewAttachedToWindow(View view) {
          if (mOnItemClickListener != null) {
            view.setOnClickListener(mOnClickListener);
          }
          if (mOnItemLongClickListener != null) {
            view.setOnLongClickListener(mOnLongClickListener);
          }
          if (mOnItemTouchListener != null) {
            view.setOnTouchListener(mOnTouchListener);
          }
          if (mOnItemFocusChangeListener != null) {
            view.setOnFocusChangeListener(mOnFocusChangeListener);
          }
          if (mOnItemKeyListener != null) {
            view.setOnKeyListener(mOnKeyListener);
          }
        }

        @Override
        public void onChildViewDetachedFromWindow(View view) {

        }
      };

  private RvItemClickSupport(RecyclerView recyclerView) {
    mRecyclerView = recyclerView;
    mRecyclerView.setTag(R.id.item_click_support, this);
    mRecyclerView.addOnChildAttachStateChangeListener(mAttachListener);
  }

  public static RvItemClickSupport addTo(RecyclerView view) {
    RvItemClickSupport support = (RvItemClickSupport) view.getTag(R.id.item_click_support);
    if (support == null) {
      support = new RvItemClickSupport(view);
    }
    return support;
  }

  public static RvItemClickSupport removeFrom(RecyclerView view) {
    RvItemClickSupport support = (RvItemClickSupport) view.getTag(R.id.item_click_support);
    if (support != null) {
      support.detach(view);
    }
    return support;
  }

  /**
   * 设置按键监听
   */
  public RvItemClickSupport setOnItemKeyListenr(OnItemKeyListener onItemKeyListener) {
    mOnItemKeyListener = onItemKeyListener;
    return this;
  }

  /**
   * 设置焦点监听
   */
  public RvItemClickSupport setOnItemFocusChangeListener(
      OnItemFocusChangeListener onItemFocusChangeListener) {
    mOnItemFocusChangeListener = onItemFocusChangeListener;
    return this;
  }

  /**
   * 设置触摸监听
   */
  public RvItemClickSupport setOnItemTouchListener(OnItemTouchListener onItemTouchListener) {
    mOnItemTouchListener = onItemTouchListener;
    return this;
  }

  /**
   * 设置点击监听
   */
  public RvItemClickSupport setOnItemClickListener(OnItemClickListener listener) {
    mOnItemClickListener = listener;
    return this;
  }

  /**
   * 设置长按监听
   */
  public RvItemClickSupport setOnItemLongClickListener(OnItemLongClickListener listener) {
    mOnItemLongClickListener = listener;
    return this;
  }

  private void detach(RecyclerView view) {
    view.removeOnChildAttachStateChangeListener(mAttachListener);
    view.setTag(R.id.item_click_support, null);
  }
}