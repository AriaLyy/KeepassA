package com.lyy.keepassa.view.detail

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupId
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.databinding.ActivityGroupDetailBinding
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.event.CreateOrUpdateEntryEvent
import com.lyy.keepassa.event.DelEvent
import com.lyy.keepassa.event.UndoEvent
import com.lyy.keepassa.util.EventBusHelper
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.view.SimpleEntryAdapter
import com.lyy.keepassa.view.create.CreateEntryActivity
import com.lyy.keepassa.view.create.CreateGroupDialog
import com.lyy.keepassa.view.menu.EntryPopMenu
import com.lyy.keepassa.view.menu.GroupPopMenu
import com.lyy.keepassa.widget.MainExpandFloatActionButton
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN

/**
 * 群组详情、回收站详情
 */
class GroupDetailActivity : BaseActivity<ActivityGroupDetailBinding>() {

  companion object {
    const val KEY_TITLE = "KEY_TITLE"
    const val KEY_GROUP_ID = "KEY_V3_GROUP_ID"
    const val KEY_IS_IN_RECYCLE_BIN = "KEY_IS_IN_RECYCLE_BIN"
  }

  private lateinit var module: DetailModule
  private lateinit var adapter: SimpleEntryAdapter
  private val entryData = ArrayList<SimpleItemEntity>()
  private var curx = 0
  private var isRecycleBin = false
  private lateinit var groupId: PwGroupId

  override fun setLayoutId(): Int {
    return R.layout.activity_group_detail
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)

    val pwId = intent.getSerializableExtra(KEY_GROUP_ID)
    if (pwId == null) {
      HitUtil.toaskShort(getString(R.string.error_group_id_null))
      finish()
      return
    }
    EventBusHelper.reg(this)
    groupId = pwId as PwGroupId

    isRecycleBin = intent.getBooleanExtra(KEY_IS_IN_RECYCLE_BIN, false)
    // 检查是否是在回收站中
    if (!isRecycleBin) {
      isRecycleBin =
        BaseApp.isV4 && BaseApp.KDB.pm.recycleBin != null && BaseApp.KDB.pm.recycleBin.id == groupId
    }

    module = ViewModelProvider(this).get(DetailModule::class.java)
    val title = intent.getStringExtra(KEY_TITLE)
    binding.ctlCollapsingLayout.title = title
    binding.kpaToolbar.title = title
    binding.kpaToolbar.setNavigationOnClickListener {
      finishAfterTransition()
    }

//    binding.toolbar.inflateMenu(R.menu.menu_group_detail)
    initList()
    initFab()
  }

  private fun initFab() {
    if (isRecycleBin) {
      binding.fab.visibility = View.GONE
      return
    }
    binding.fab.setOnItemClickListener(object : MainExpandFloatActionButton.OnItemClickListener {
      override fun onKeyClick() {
        startActivity(
            Intent(this@GroupDetailActivity, CreateEntryActivity::class.java).apply {
              putExtra(CreateEntryActivity.KEY_TYPE, CreateEntryActivity.TYPE_NEW_ENTRY)
              putExtra(CreateEntryActivity.PARENT_GROUP_ID, groupId)
            },
            ActivityOptions.makeSceneTransitionAnimation(this@GroupDetailActivity)
                .toBundle()
        )

        binding.fab.hintMoreOperate()
      }

      override fun onGroupClick() {
        val dialog = CreateGroupDialog.generate {
          parentGroup = BaseApp.KDB.pm.rootGroup
          build()
        }
        dialog.show(supportFragmentManager, "CreateGroupDialog")
        binding.fab.hintMoreOperate()
      }

    })
  }

  private fun initList() {
    adapter = SimpleEntryAdapter(this, entryData)
    binding.list.setHasFixedSize(true)
    binding.list.layoutManager = LinearLayoutManager(this)
    binding.list.adapter = adapter

    getData()

    RvItemClickSupport.addTo(binding.list)
        .setOnItemClickListener { _, position, v ->
          val item = entryData[position]
          if (item.obj is PwGroup) {
            val group = item.obj as PwGroup
            val intent = Intent(this, GroupDetailActivity::class.java)
            intent.putExtra(KEY_GROUP_ID, group.id)
            intent.putExtra(KEY_TITLE, group.name)
            intent.putExtra(KEY_IS_IN_RECYCLE_BIN, isRecycleBin)
            startActivity(
                intent, ActivityOptions.makeSceneTransitionAnimation(this)
                .toBundle()
            )
          } else if (item.obj is PwEntry) {
            val icon = v.findViewById<AppCompatImageView>(R.id.icon)
            KeepassAUtil.turnEntryDetail(this, item.obj as PwEntry, icon)
          }
        }

    RvItemClickSupport.addTo(binding.list)
        .setOnItemLongClickListener { _, position, v ->
          showPopMenu(v, position)
          true
        }

    // 获取点击位置
    binding.list.addOnItemTouchListener(object : OnItemTouchListener {
      override fun onTouchEvent(
        rv: RecyclerView,
        e: MotionEvent
      ) {

      }

      override fun onInterceptTouchEvent(
        rv: RecyclerView,
        e: MotionEvent
      ): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) {
          curx = e.x.toInt()
        }
        return false
      }

      override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
      }

    })
  }

  private fun getData() {
    module.getGroupData(this, groupId)
        .observe(this, Observer { list ->
          if (list == null || list.size == 0) {
            // 设置appbar为收缩状态
            binding.appBar.setExpanded(false, false)
            binding.emptyLayout.visibility = View.VISIBLE
            binding.list.visibility = View.GONE
            binding.emptyLayout.translationY = resources.getDimension(R.dimen.bar_height)
            return@Observer
          } else {
            binding.emptyLayout.visibility = View.GONE
            binding.list.visibility = View.VISIBLE
          }
          entryData.clear()
          entryData.addAll(list)
          adapter.notifyDataSetChanged()
        })
  }

  /**
   * 创建条目
   */
  @Subscribe(threadMode = MAIN)
  fun onEntryCreate(event: CreateOrUpdateEntryEvent) {
    if (event.entry.parent.id == groupId) {
      if (event.isUpdate) {
        val entry: SimpleItemEntity? = entryData.find { it.obj == event.entry }
        entry?.let {
          val pos = entryData.indexOf(it)
          entryData[pos] = KeepassAUtil.convertPwEntry2Item(event.entry)
          adapter.notifyItemChanged(pos)
        }
      } else {
        getData()
      }
    }
  }

  /**
   * 删除群组
   */
  @Subscribe(threadMode = MAIN)
  fun onDelGroup(delEvent: DelEvent?) {
    if (delEvent == null) {
      return
    }
    getData()
  }

  /**
   * 回收站中需要删除数据
   */
  @Subscribe(threadMode = MAIN)
  fun onUndo(event: UndoEvent) {
    if (!isRecycleBin) {
      return
    }
    getData()
  }

  /**
   * 长按悬浮菜单
   */
  private fun showPopMenu(
    v: View,
    position: Int
  ) {
    val data = entryData[position]
    if (data.obj is PwGroup) {
      val group = data.obj as PwGroup
      val pop = GroupPopMenu(this, v, group, curx, isRecycleBin)
      pop.show()

    } else if (data.obj is PwEntry) {
      val entry = data.obj as PwEntry
      val pop = EntryPopMenu(this, v, entry, curx, isRecycleBin)
      pop.show()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    EventBusHelper.unReg(this)
  }

}