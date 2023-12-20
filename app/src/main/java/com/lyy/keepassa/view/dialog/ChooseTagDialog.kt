/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.dialog

import android.view.View
import android.widget.CheckBox
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogChooseTagBinding
import com.lyy.keepassa.entity.TagBean
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.doOnItemClickListener
import com.lyy.keepassa.util.loadImg
import com.lyy.keepassa.view.create.CreateEntryModule
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/**
 * @Author laoyuyu
 * @Description
 * @Date 10:39 AM 2023/10/26
 **/
@Route(path = "/dialog/chooseTag")
internal class ChooseTagDialog : BaseDialog<DialogChooseTagBinding>() {
  companion object {
    val chooseTagFlow = MutableSharedFlow<List<TagBean>>(1)
    val ADD_MORE = TagBean(ResUtil.getString(R.string.create_tag))
  }

  private lateinit var module: CreateEntryModule

  @Autowired(name = "entry")
  @JvmField
  var entry: PwEntryV4? = null

  @Autowired(name = "newTag")
  @JvmField
  var newTag: TagBean? = null

  private val tagList = mutableListOf<TagBean>()

  override fun setLayoutId(): Int {
    return R.layout.dialog_choose_tag
  }

  override fun initData() {
    super.initData()
    ARouter.getInstance().inject(this)
    module = ViewModelProvider(requireActivity())[CreateEntryModule::class.java]
    binding.msgTitle = ResUtil.getString(R.string.add_tag)
    val tagAdapter = TagAdapter()
    binding.enableEnterBt = true
    lifecycleScope.launch {
      binding.rvList.apply {
        setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context)
        adapter = tagAdapter
      }
      tagList.addAll(getTagData().toMutableList())
      tagAdapter.setNewInstance(tagList)
    }
    binding.rvList.doOnItemClickListener { _, position, _ ->
      val tagBean = tagList[position]
      if (tagBean == ADD_MORE) {
        module.cacheTag(tagList)
        dismiss()
        Routerfit.create(DialogRouter::class.java).showCreateTagDialog()
        return@doOnItemClickListener
      }
      tagBean.isSet = !tagBean.isSet
      tagAdapter.notifyItemChanged(position, tagBean.isSet)
    }
    binding.clicker = object : DialogBtnClicker {
      override fun onEnter(v: View) {
        lifecycleScope.launch {
          chooseTagFlow.emit(tagList.filter { it.isSet })
          dismiss()
        }
      }

      override fun onCancel(v: View) {
        dismiss()
      }
    }
  }

  private suspend fun getTagData(): List<TagBean> {
    val tagList = arrayListOf<TagBean>()
    entry?.let {
      val curTagList = KdbUtil.getEntryTag(it)
      val allTagList = KdbUtil.getAllTags()
      allTagList.forEach { tag ->
        tagList.add(TagBean(tag, tag in curTagList || tag in module.selectedTagBeanCache))
      }
    }
    newTag?.let { tagList.add(it) }
    tagList.add(ADD_MORE)
    return tagList
  }

  private class TagAdapter : BaseQuickAdapter<TagBean, BaseViewHolder>(R.layout.item_choose_tag) {
    override fun convert(holder: BaseViewHolder, item: TagBean, payloads: List<Any>) {
      super.convert(holder, item, payloads)
      holder.getView<CheckBox>(R.id.cb).isChecked = item.isSet
    }

    override fun convert(holder: BaseViewHolder, item: TagBean) {
      holder.setVisible(R.id.cb, item != ADD_MORE)
      holder.setText(R.id.tvTitle, item.tag)
      holder.getView<AppCompatImageView>(R.id.ivIcon)
        .loadImg(if (item == ADD_MORE) R.drawable.ic_add_24px else R.drawable.ic_baseline_label_24)
      holder.getView<CheckBox>(R.id.cb).isChecked = item.isSet
    }
  }
}