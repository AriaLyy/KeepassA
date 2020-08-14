package com.lyy.demo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arialyy.frame.core.AbsActivity
import com.arialyy.frame.util.adapter.AbsHolder
import com.arialyy.frame.util.adapter.AbsRVAdapter
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.lyy.demo.MainActivity.Adapter.Holder
import com.lyy.demo.databinding.ActivityMainBinding

class MainActivity : AbsActivity<ActivityMainBinding>() {

  private val strings = mutableListOf("uri权限管理", "指纹加密数据", "指纹校验", "富文本", "注册码")

  override fun setLayoutId(): Int {
    return R.layout.activity_main
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    val adapter = Adapter(this, strings)
    val list = findViewById<RecyclerView>(R.id.list)
    list.adapter = adapter
    list.layoutManager = LinearLayoutManager(this)
    list.setHasFixedSize(true)
    adapter.notifyDataSetChanged()
    RvItemClickSupport.addTo(list).setOnItemClickListener { _, position, _ ->
      when (position) {
        0 -> { // uri权限管理
          startActivity(Intent(this, UriActivity::class.java))
        }
        1 -> { // 指纹加密数据
          startActivity(Intent(this, FingerprintActivity::class.java))
        }
        2 -> { // 指纹校验
          startActivity(Intent(this, FingerprintVerifyActivity::class.java))
        }
        3 -> { // 富文本
          startActivity(Intent(this, RichTextActivity::class.java))
        }
        4 -> { // 注册码
          startActivity(Intent(this, RegCodeActivity::class.java))
        }
      }
    }
  }

  private class Adapter(
    context: Context,
    datas: List<String>
  ) : AbsRVAdapter<String, Holder>(context, datas) {

    private class Holder(view: View) : AbsHolder(view) {
      val tv: TextView = view.findViewById(R.id.tv)
    }

    override fun getViewHolder(
      convertView: View,
      viewType: Int
    ): Holder {
      return Holder(convertView)
    }

    override fun setLayoutId(type: Int): Int {
      return R.layout.item_funcation
    }

    override fun bindData(
      holder: Holder,
      position: Int,
      item: String
    ) {
      holder.tv.text = item
    }

  }

}
