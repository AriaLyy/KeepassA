package com.lyy.keepassa.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.uiwidget.R

/**
 * 布局中嵌套RecyclerView，并且内容随着RecyclerView滚动而滚动的，类似BottomSheetDialog的Behavior
 */
public class RecyclerViewContentBehavior(
  context: Context,
  attrs: AttributeSet
) : CoordinatorLayout.Behavior<View>(context, attrs) {
  private val TAG = javaClass.simpleName
  private var anim: ObjectAnimator? = null
  // 收缩状态的坐标
  private var collapsedY = 0f
  // 自动全屏的坐标 cY < expandY：全屏展开。 expandY < cY < collapsedY：恢复收缩状态
  private var expandY = 0f
  // 关闭的坐标 expandY < cY < hintY：进入收缩状态。hintY > cY：关闭
  private var hintY = 0f
  private var expandH = 500f
  private var isFling = false
  private var listener: OnDragStateListener? = null
  private lateinit var child: View
  private lateinit var context: Context

  companion object {
    /** The bottom sheet is expanded.  */
    const val STATE_EXPANDED = 3

    /** The bottom sheet is collapsed.  */
    const val STATE_COLLAPSED = 4

    /** The bottom sheet is hidden.  */
    const val STATE_HIDDEN = 5
  }

  interface OnDragStateListener {
    fun onStatce(state: Int)
  }

  init {
    val ta = context.obtainStyledAttributes(attrs, R.styleable.RecyclerViewContentBehavior_Layout)
    expandH = ta.getDimension(
        R.styleable.RecyclerViewContentBehavior_Layout_expand_h, 500.toPx().toFloat()
    )
    ta.recycle()
    this.context = context
  }

  public fun setOnDragStateListener(listener: OnDragStateListener) {
    this.listener = listener
  }

  override fun onInterceptTouchEvent(
    parent: CoordinatorLayout,
    child: View,
    ev: MotionEvent
  ): Boolean {
    when (ev.action) {
      MotionEvent.ACTION_DOWN -> {
        isFling = false
      }
      MotionEvent.ACTION_MOVE, MotionEvent.ACTION_CANCEL -> {
        if (anim != null && anim!!.isRunning) {
          anim!!.cancel()
        }
      }
    }
    return super.onInterceptTouchEvent(parent, child, ev)
  }

  override fun onMeasureChild(
    parent: CoordinatorLayout,
    child: View,
    parentWidthMeasureSpec: Int,
    widthUsed: Int,
    parentHeightMeasureSpec: Int,
    heightUsed: Int
  ): Boolean {

    return super.onMeasureChild(
        parent, child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed
    )
  }

  override fun onLayoutChild(
    parent: CoordinatorLayout,
    child: View,
    layoutDirection: Int
  ): Boolean {
    if (child.height > 0) {
      collapsedY = child.height - expandH
      expandY = child.height - (expandH + expandH / 2)
      hintY = collapsedY + expandH / 2
      Log.d(
          TAG,
          "cHeight = ${child.height}, expandY = $collapsedY, fullExpandY = $expandY, hintY = $hintY"
      )
//      child.translationY = collapsedY
      val enterAnim =
        ObjectAnimator.ofFloat(child, "translationY", child.height.toFloat(), collapsedY)
      enterAnim.duration = 400
      enterAnim.start()
    }else{
//      Log.d(TAG, "childH = 0")
      child.translationY = getScreenHeight(context).toFloat()
    }
    this.child = child

    return super.onLayoutChild(parent, child, layoutDirection)
  }

  fun getScreenHeight(context: Context): Int {
    val wm = context
        .getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val outMetrics = DisplayMetrics()
    wm.defaultDisplay
        .getMetrics(outMetrics)
    return outMetrics.heightPixels
  }

  /**
   *  当coordinatorLayout 的子View视图开始嵌套滑动的时候被调用。
   *  当返回值为true的时候表明 coordinatorLayout 充当nested scroll parent 处理这次滑动
   *  需要注意的是只有当返回值为true的时候，Behavior 才能收到后面的一些nested scroll 事件回调（如：onNestedPreScroll、onNestedScroll等）
   *  这个方法有个重要的参数axes，表明处理的滑动的方向。
   *
   * @param coordinatorLayout 和Behavior 绑定的View的父CoordinatorLayout
   * @param child  和Behavior 绑定的View
   * @param directTargetChild
   * @param target           滚动的view
   * @param axes 嵌套滑动 应用的滑动方向，看 {@link ViewCompat#SCROLL_AXIS_HORIZONTAL},
   *                         {@link ViewCompat#SCROLL_AXIS_VERTICAL}
   * @return
   */
  override fun onStartNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: View,
    directTargetChild: View,
    target: View,
    axes: Int,
    type: Int
  ): Boolean {
    return axes == ViewCompat.SCROLL_AXIS_VERTICAL
  }

  /**
   * 嵌套滚动发生之前被调用
   * 在nested scroll child 消费掉自己的滚动距离之前，嵌套滚动每次被nested scroll child
   * 更新都会调用onNestedPreScroll。注意有个重要的参数consumed，可以修改这个数组表示你消费
   * 了多少距离。假设用户滑动了100px,child 做了90px 的位移，你需要把 consumed［1］的值改成90，
   * 这样coordinatorLayout就能知道只处理剩下的10px的滚动。
   * @param coordinatorLayout
   * @param child
   * @param target
   * @param dx  用户水平方向的滚动距离
   * @param dy  用户竖直方向的滚动距离, 小于0：往下滑动
   * @param consumed
   */
  override fun onNestedPreScroll(
    coordinatorLayout: CoordinatorLayout,
    child: View,
    target: View,
    dx: Int,
    dy: Int,
    consumed: IntArray,
    type: Int
  ) {

    if (target is RecyclerView) {
      if (!target.canScrollVertically(-1)
          && dy < 0
          && !isFling
      ) {
        child.translationY += (-dy)
      } else if (dy > 0 && child.translationY.toInt() != 0) {
        child.translationY -= dy
        if (child.translationY < 0) {
          child.translationY = 0f
        }
        consumed[1] = dy // 往上移动时，将所有的dy都消耗掉，这样recyclerVeiw就不会进行滚动了
      }
    }
  }

  override fun onNestedPreFling(
    coordinatorLayout: CoordinatorLayout,
    child: View,
    target: View,
    velocityX: Float,
    velocityY: Float
  ): Boolean {
    isFling = true
    return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY)
  }

  override fun onStopNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: View,
    target: View,
    type: Int
  ) {
//      Log.d(TAG, "y = ${child.translationY}")
    var state = STATE_EXPANDED
    if (child.translationY < expandY) {
      // 全屏展开
      moveViewAnim(child, 0f)
      state = STATE_EXPANDED
    } else if (expandY < child.translationY && child.translationY < collapsedY) {
      // 进入收缩状态
      moveViewAnim(child, collapsedY)
      state = STATE_COLLAPSED
    } else if (collapsedY < child.translationY && child.translationY < hintY) {
      // 进入收缩状态
      moveViewAnim(child, collapsedY)
      state = STATE_COLLAPSED
    } else if (hintY < child.translationY) {
      // 进入关闭状态
      moveViewAnim(child, child.height.toFloat())
      state = STATE_HIDDEN
    }

    if (listener != null) {
      listener!!.onStatce(state)
    }
  }

  /**
   * 设置状态，并根据状态设置布局位置
   */
  public fun setState(state: Int) {
    when (state) {
      STATE_EXPANDED -> setStateAnim(child, 0f, state)
      STATE_COLLAPSED -> setStateAnim(child, collapsedY, state)
      STATE_HIDDEN -> setStateAnim(child, child.height.toFloat(), state)
    }
  }

  var stateAnim: ObjectAnimator? = null
  fun setStateAnim(
    child: View,
    endL: Float,
    state: Int
  ) {
    if (stateAnim != null && stateAnim!!.isRunning) {
      stateAnim!!.cancel()
    }
    stateAnim = ObjectAnimator.ofFloat(child, "translationY", child.translationY, endL)
    stateAnim!!.duration = 400
    stateAnim!!.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationEnd(animation: Animator?) {
        super.onAnimationEnd(animation)
        if (listener != null) {
          listener!!.onStatce(state)
        }
      }
    })
    stateAnim!!.start()

  }

  fun moveViewAnim(
    child: View,
    endL: Float
  ) {
    if (anim != null && anim!!.isRunning) {
      anim!!.cancel()
    }
    anim = ObjectAnimator.ofFloat(child, "translationY", child.translationY, endL)
    anim!!.duration = 400
    anim!!.start()
  }

}