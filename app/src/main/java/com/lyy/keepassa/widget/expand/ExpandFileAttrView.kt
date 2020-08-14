package com.lyy.keepassa.widget.expand

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import com.arialyy.frame.util.StringUtil
import com.keepassdroid.database.security.ProtectedBinary

/**
 * 可展开的附件view
 */
class ExpandFileAttrView(
  context: Context,
  attr: AttributeSet
) : LinearLayout(context, attr), View.OnClickListener {

  private val TAG = javaClass.simpleName
  private val data = LinkedHashMap<String, ProtectedBinary?>()
  private val childMap = LinkedHashMap<String, AttrFileItemView>()
  private var expandAnim: ValueAnimator? = null
  private var hintAnim: ValueAnimator? = null
  private val animDuring = 200L
  private var strViewListener: OnAttrFileViewClickListener? = null

  /**
   * 高级属性的view点击事件
   */
  public interface OnAttrFileViewClickListener {
    fun onClickListener(
      v: AttrFileItemView,
      key: String,
      position: Int
    )
  }

  init {
    orientation = VERTICAL
  }

  override fun removeAllViews() {
    data.clear()
    childMap.clear()
    super.removeAllViews()
  }

  /**
   * 设置高级属性的view点击事件
   */
  public fun setOnAttrFileViewClickListener(listener: OnAttrFileViewClickListener) {
    this.strViewListener = listener
  }

  fun setValue(map: LinkedHashMap<String, ProtectedBinary>) {
    data.putAll(map)
    if (data.isNotEmpty()) {
      var i = 0
      for (d in data) {
        val child = AttrFileItemView(context, d.key, d.value)
        child.setOnClickListener(this)
        child.isClickable = true
        addView(child, i)
        childMap[d.key] = child
        i++
      }
    }
    invalidate()
  }

  fun addValue(
    key: String,
    value: ProtectedBinary? = null,
    fileUri: Uri? = null
  ) {
    data[key] = value
    val child = AttrFileItemView(context, key, value, fileUri)
    child.setOnClickListener(this)
    addView(child, childCount)
    childMap[key] = child
    invalidate()
  }

  fun removeValue(key: String) {
    if (key.isEmpty() || !data.keys.contains(key)) {
      Log.e(TAG, "key【$key】错误")
      return
    }
    removeView(childMap[key])
    childMap.remove(key)
    invalidate()
  }

  /**
   * 更新数据
   */
  fun updateKeyValue(
    v: AttrFileItemView,
    key: String,
    value: ProtectedBinary
  ) {
    v.updateData(key, value)
    invalidate()
  }

  /**
   * 展开
   */
  public fun expand() {
    if (hintAnim != null && hintAnim!!.isRunning) {
      hintAnim!!.cancel()
    }
    alpha = 0f
    visibility = View.VISIBLE
    if (expandAnim == null) {
      expandAnim = ValueAnimator.ofFloat(0f, 1f)
      expandAnim!!.addUpdateListener { animation ->
        alpha = animation.animatedValue as Float
        requestLayout()
      }
    }
    expandAnim!!.duration = animDuring
    expandAnim!!.start()
  }

  /**
   * 隐藏
   */
  public fun hint() {
    if (expandAnim != null && expandAnim!!.isRunning) {
      expandAnim!!.cancel()
    }
    if (hintAnim == null) {
      hintAnim = ValueAnimator.ofFloat(1.0f, 0f)
      hintAnim!!.addUpdateListener { animation ->
        alpha = animation.animatedValue as Float
        requestLayout()
      }
      hintAnim!!.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
          super.onAnimationEnd(animation)
          visibility = View.GONE
        }
      })
    }
    hintAnim!!.duration = animDuring
    hintAnim!!.start()
  }

  override fun onClick(v: View?) {
    if (strViewListener != null) {
      strViewListener!!.onClickListener(
          v!! as AttrFileItemView, (v as AttrFileItemView).titleStr, childMap.values.indexOf(v)
      )
    }
  }

}