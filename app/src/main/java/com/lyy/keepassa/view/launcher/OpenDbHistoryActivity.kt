package com.lyy.keepassa.view.launcher

import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.databinding.ActivityOnlyListBinding
import com.lyy.keepassa.entity.DbRecord
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.event.ChangeDbEvent
import com.lyy.keepassa.view.DbPathType
import com.lyy.keepassa.view.SimpleAdapter
import org.greenrobot.eventbus.EventBus

/**
 * 数据库打开记录列表
 */
class OpenDbHistoryActivity : BaseActivity<ActivityOnlyListBinding>() {

  private val data: ArrayList<SimpleItemEntity> = ArrayList()
  private lateinit var adapter: SimpleAdapter
  private lateinit var module: LauncherModule

  override fun setLayoutId(): Int {
    return R.layout.activity_only_list
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    module = ViewModelProvider(this).get(LauncherModule::class.java)
    toolbar.title = getString(R.string.history_record)
    adapter = SimpleAdapter(this, data)
    binding.list.layoutManager = LinearLayoutManager(this)
    binding.list.setHasFixedSize(true)
    binding.list.adapter = adapter

    module.getDbOpenRecordList(this)
        .observe(this, Observer { list ->
          if (list != null && list.isNotEmpty()) {
            data.addAll(list)
            adapter.notifyDataSetChanged()
          }
          if (data.size > 0) {
            binding.temp.visibility = View.GONE
          } else {
            binding.temp.visibility = View.VISIBLE
          }
        })


    RvItemClickSupport.addTo(binding.list)
        .setOnItemClickListener { _, position, _ ->
          val record = data[position].obj as DbRecord
          finishAfterTransition()
          EventBus.getDefault()
              .post(
                  ChangeDbEvent(
                      dbName = record.dbName,
                      localFileUri = Uri.parse(record.localDbUri),
                      cloudPath = record.cloudDiskPath,
                      uriType = DbPathType.valueOf(record.type),
                      keyUri = if (TextUtils.isEmpty(record.keyUri)) null else Uri.parse(
                          record.keyUri
                      )
                  )
              )
        }
  }

  override fun onDestroy() {
    super.onDestroy()
  }

}