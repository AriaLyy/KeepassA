package com.lyy.keepassa.view.detail

import android.graphics.Paint
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProvider
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.databinding.ActivityEntryDetailNewBinding
import com.lyy.keepassa.router.ActivityRouter
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.doClick
import com.lyy.keepassa.util.loadImg
import timber.log.Timber
import java.util.Date
import java.util.UUID
import kotlin.math.abs

/**
 * @Author laoyuyu
 * @Description
 * @Date 5:24 PM 2023/9/26
 **/
@Route(path = "/entry/detail")
class EntryDetailActivityNew : BaseActivity<ActivityEntryDetailNewBinding>() {
  companion object {
    const val KEY_ENTRY_ID = "KEY_ENTRY_ID"
  }

  private lateinit var module: EntryDetailModule
  private lateinit var pwEntry: PwEntryV4
  private var isInRecycleBin = false

  @Autowired(name = KEY_ENTRY_ID)
  lateinit var uuid: UUID

  override fun setLayoutId(): Int {
    return R.layout.activity_entry_detail_new
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    ARouter.getInstance().inject(this)
    module = ViewModelProvider(this)[EntryDetailModule::class.java]
    pwEntry = (BaseApp.KDB!!.pm.entries[uuid] as PwEntryV4?)!!
    module.initEntry(pwEntry)
    if (BaseApp.isV4 && pwEntry.parent == BaseApp.KDB!!.pm.recycleBin) {
      isInRecycleBin = true
    }
    setTopBar()
    setIcon()
    binding.cardBaseInfo.bindData(pwEntry)
    binding.cardNote.bindData(pwEntry)
    binding.cardStr.bindData(pwEntry)
    binding.cardAtta.bindData(pwEntry)
    binding.cardTag.bindData(pwEntry)
  }

  /**
   * 标题栏
   */
  private fun setTopBar(){
    binding.topAppBar.title = pwEntry.title
    toolbar = binding.topAppBar
    toolbar.setNavigationOnClickListener {
      finishAfterTransition()
    }
    toolbar.inflateMenu(R.menu.menu_entry_detail)
    toolbar.setOnMenuItemClickListener { item ->
      if (KeepassAUtil.instance.isFastClick()) {
        return@setOnMenuItemClickListener true
      }
      when(item.itemId){
        R.id.edit -> {
          Routerfit.create(ActivityRouter::class.java, this).toEditEntryActivity(
            pwEntry.uuid,
            ActivityOptionsCompat.makeSceneTransitionAnimation(this)
          )
        }
      }

      true
    }
    binding.tvTitle.text = pwEntry.title
    binding.appBarLayout.addOnOffsetChangedListener { _, verticalOffset ->
      // Timber.d("offset: $verticalOffset， ${binding.appBarLayout.totalScrollRange}")
      if (verticalOffset == 0){
        binding.topAppBar.title = ""
        return@addOnOffsetChangedListener
      }
      if (abs(verticalOffset) >= binding.appBarLayout.totalScrollRange){
        binding.topAppBar.title = pwEntry.title
        return@addOnOffsetChangedListener
      }
    }
    handleMenuBar()

    // 处理过期
    if (pwEntry.expiryTime.before(Date(System.currentTimeMillis()))) {
      val paint = binding.tvTitle.paint
      paint.flags = Paint.STRIKE_THRU_TEXT_FLAG
      paint.isAntiAlias = true
    }
  }

  private fun handleMenuBar(){
    binding.btnUserName.doClick {
      Timber.d("userName")
    }
  }

  /**
   * 设置图标
   */
  private fun setIcon() {
    if (pwEntry.getCustomIcon().imageData.isNotEmpty()) {
      binding.ivIcon.loadImg(IconUtil.getCustomBitmap(pwEntry))
      return
    }
    if (pwEntry.title.isNullOrBlank()) {
      IconUtil.setEntryIcon(pwEntry, binding.ivIcon)
      return
    }
    binding.tvChar.visibility = View.VISIBLE
    binding.tvChar.text = pwEntry.title.substring(0, 1)
    binding.ivIcon.setBackgroundColor(ResUtil.getColor(R.color.color_444E85DB))
  }
}