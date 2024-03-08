package com.lyy.keepassa.base

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

class ViewBindingVH<V : ViewBinding>(val b: V) :
  ViewHolder(b.root)

abstract class AbsViewBindingAdapter<T, V : ViewBinding> :
  RecyclerView.Adapter<ViewBindingVH<V>>() {
  var data: MutableList<T> = mutableListOf()
    internal set
  lateinit var context: Context

  // 通过反射创建ViewBinding
  private fun viewBinding(parent: ViewGroup): V {
    val parameterizedType = this.javaClass.genericSuperclass as ParameterizedType
    val clazz: Class<V> = parameterizedType.actualTypeArguments[1] as Class<V>
    val inflateMethod = clazz.getMethod(
      "inflate",
      LayoutInflater::class.java,
      ViewGroup::class.java,
      Boolean::class.java
    )
    return inflateMethod.invoke(null, LayoutInflater.from(parent.context), parent, false) as V
  }

  fun setData(list: MutableList<T>) {
    data = list
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBindingVH<V> {
    return ViewBindingVH(viewBinding(parent))
  }

  override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    super.onAttachedToRecyclerView(recyclerView)
    context = recyclerView.context
  }

  override fun getItemCount(): Int {
    return data.size
  }

  abstract fun bindData(binding: V, item: T)

  open fun bindData(binding: V, item: T, payloads: MutableList<Any>) {}

  override fun onBindViewHolder(holder: ViewBindingVH<V>, position: Int) {
    bindData(holder.b, data[position])
  }

  override fun onBindViewHolder(
    holder: ViewBindingVH<V>,
    position: Int,
    payloads: MutableList<Any>
  ) {
    super.onBindViewHolder(holder, position, payloads)
    bindData(holder.b, data[position], payloads)
  }
}

interface IMultipleItem {
  fun getType(): Int
}

interface OnMultiItemAdapterListener<TYPE : IMultipleItem, ViewHolder : RecyclerView.ViewHolder> {
  fun onCreate(context: Context, parent: ViewGroup, viewType: Int): ViewHolder

  fun onBind(holder: ViewHolder, position: Int, item: TYPE)

  fun onDetachedFromWindow(holder: ViewHolder) {}
}

abstract class AbsMultipleViewBindingAdapter :
  RecyclerView.Adapter<ViewHolder>() {
  private val typeMap =
    hashMapOf<Int, OnMultiItemAdapterListener<IMultipleItem, ViewHolder>>()
  var data: MutableList<IMultipleItem> = mutableListOf()
    internal set

  fun setData(list: MutableList<IMultipleItem>) {
    data = list
    notifyDataSetChanged()
  }

  fun <T : IMultipleItem, V : ViewHolder> addItemType(
    type: Int,
    adapter: OnMultiItemAdapterListener<T, V>
  ) {
    typeMap[type] = adapter as OnMultiItemAdapterListener<IMultipleItem, ViewHolder>
  }

  override fun getItemViewType(position: Int): Int {
    if (data.isEmpty()) {
      return -1
    }
    return data[position].getType()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    if (viewType == -1) {
      throw IllegalArgumentException("viewType类型错误")
    }
    return typeMap[viewType]!!.onCreate(parent.context, parent, viewType)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    typeMap[getItemViewType(position)]!!.onBind(holder, position, data[position])
  }

  override fun getItemCount(): Int {
    return data.size
  }

  override fun onViewDetachedFromWindow(holder: ViewHolder) {
    super.onViewDetachedFromWindow(holder)
    typeMap[getItemViewType(holder.absoluteAdapterPosition)]?.onDetachedFromWindow(holder)
  }
}