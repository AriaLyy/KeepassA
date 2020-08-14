package com.lyy.keepassa.view.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.LinearLayoutManager
import com.arialyy.frame.util.adapter.AbsHolder
import com.arialyy.frame.util.adapter.AbsRVAdapter
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.bumptech.glide.Glide
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseBottomSheetDialogFragment
import com.lyy.keepassa.databinding.DialogAddMoreBinding
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.view.dialog.AddMoreDialog.Adapter.Holder

/**
 * 添加更多
 */
class AddMoreDialog(val data: List<SimpleItemEntity>) : BaseBottomSheetDialogFragment<DialogAddMoreBinding>() {

  private lateinit var adapter: Adapter
  private var listener: OnItemClickListener? = null

  override fun setLayoutId(): Int {
    return R.layout.dialog_add_more
  }

  interface OnItemClickListener {
    fun onItemClick(
      position: Int,
      item: SimpleItemEntity,
      view: View
    )
  }

  override fun init(savedInstanceState: Bundle?) {
    super.init(savedInstanceState)
    adapter = Adapter(context!!, data)
    binding.list.adapter = adapter
    binding.list.setHasFixedSize(true)
    binding.list.layoutManager = LinearLayoutManager(context)
    RvItemClickSupport.addTo(binding.list)
        .setOnItemClickListener { recyclerView, position, v ->
          listener?.onItemClick(position, data[position], v)
        }
  }

  public fun setOnItemClickListener(listener: OnItemClickListener) {
    this.listener = listener
  }

  public fun notifyData() {
    adapter.notifyDataSetChanged()
  }

  /**
   * 适配器
   */
  private class Adapter(
    context: Context,
    data: List<SimpleItemEntity>
  ) : AbsRVAdapter<SimpleItemEntity, Holder>(context, data) {

    private class Holder(view: View) : AbsHolder(view) {
      val img: AppCompatImageView = view.findViewById(R.id.img)
      val text: TextView = view.findViewById(R.id.text)
    }

    override fun getViewHolder(
      convertView: View?,
      viewType: Int
    ): Holder {
      return Holder(convertView!!)
    }

    override fun setLayoutId(type: Int): Int {
      return R.layout.item_simple
    }

    override fun bindData(
      holder: Holder?,
      position: Int,
      item: SimpleItemEntity?
    ) {
      holder!!.text.text = item!!.title
      Glide.with(context)
          .load(item.icon)
          .into(holder.img)
    }
  }
}