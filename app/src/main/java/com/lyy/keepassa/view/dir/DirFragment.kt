package com.lyy.keepassa.view.dir

import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupId
import com.keepassdroid.database.PwGroupV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseFragment
import com.lyy.keepassa.databinding.FragmentOnlyListBinding
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.doOnItemClickListener
import com.lyy.keepassa.view.SimpleEntryAdapter

@Route(path = "/group/choose/dir")
class DirFragment : BaseFragment<FragmentOnlyListBinding>() {
  private lateinit var adapter: SimpleEntryAdapter
  private val entryData = ArrayList<SimpleItemEntity>()

  @Autowired(name = KEY_CUR_GROUP)
  lateinit var curGroup: PwGroup

  @Autowired(name = KEY_IS_MOVE_GROUP)
  @JvmField
  var isMoverGroup = false

  @Autowired(name = KEY_IS_RECYCLE_GROUP_ID)
  @JvmField
  var recycleGroupId: PwGroupId? = null

  companion object {
    const val KEY_CUR_GROUP = "KEY_CUR_GROUP"
    const val KEY_IS_MOVE_GROUP = "KEY_IS_MOVE_GROUP"
    const val KEY_IS_RECYCLE_GROUP_ID = "KEY_IS_RECYCLE_GROUP_ID"
  }

  fun build(): DirFragment {
    return this
  }

  override fun initData() {
    ARouter.getInstance().inject(this)
    adapter = SimpleEntryAdapter(requireContext(), entryData)
    binding.list.setHasFixedSize(true)
    binding.list.layoutManager = LinearLayoutManager(context)
    binding.list.adapter = adapter

    entryData.clear()
    for (group in curGroup.childGroups) {
      if (group == BaseApp.KDB.pm.recycleBin) {
        continue
      }
      if (isMoverGroup && group.id == recycleGroupId) {
        continue
      }
      val item = SimpleItemEntity()
      item.title = group.name
      item.subTitle =
        requireContext().getString(
          R.string.hint_group_desc, KdbUtil.getGroupAllEntryNum(group)
            .toString()
        )
      item.obj = group
      entryData.add(item)
    }
    adapter.notifyDataSetChanged()

    binding.list.doOnItemClickListener { _, position, _ ->
      (activity as ChooseGroupActivity).startNextFragment(entryData[position].obj as PwGroupV4)
    }
  }

  override fun setLayoutId(): Int {
    return R.layout.fragment_only_list
  }

}