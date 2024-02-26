/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.dialog

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.AbsViewBindingAdapter
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogChooseTagBinding
import com.lyy.keepassa.databinding.ItemChooseTagBinding
import com.lyy.keepassa.entity.TagBean
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.doOnItemClickListener
import com.lyy.keepassa.util.loadImg
import com.lyy.keepassa.view.create.entry.CreateEntryModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
      withContext(Dispatchers.IO){
        tagList.addAll(getTagData().toMutableList())
      }
      tagAdapter.setData(tagList)
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
    val allTagList = KdbUtil.getAllTags()
    entry?.let {
      val curTagList = KdbUtil.getEntryTag(it)
      allTagList.forEach { tag ->
        tagList.add(TagBean(tag, tag in curTagList || tag in module.selectedTagBeanCache))
      }
    }
    newTag?.let {
      if (!allTagList.contains(it.tag)){
        tagList.add(it)
      }
    }
    tagList.add(ADD_MORE)
    return tagList
  }

  private class TagAdapter : AbsViewBindingAdapter<TagBean, ItemChooseTagBinding>() {
    override fun bindData(
      binding: ItemChooseTagBinding,
      item: TagBean,
      payloads: MutableList<Any>
    ) {
      super.bindData(binding, item, payloads)
      binding.cb.isChecked = item.isSet
    }

    override fun bindData(binding: ItemChooseTagBinding, item: TagBean) {
      binding.cb.isVisible = item != ADD_MORE
      binding.tvTitle.text = item.tag
      binding.ivIcon.loadImg(if (item == ADD_MORE) R.drawable.ic_add_24px else R.drawable.ic_baseline_label_24)
      binding.cb.isChecked = item.isSet
    }
  }
}