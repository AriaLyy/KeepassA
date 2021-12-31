/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.create

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.ViewAnimationUtils
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.transition.Transition
import androidx.transition.TransitionInflater
import com.alibaba.android.arouter.facade.annotation.Route
import com.arialyy.frame.router.Routerfit
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.databinding.ActivityCreateDbBinding
import com.lyy.keepassa.router.ActivityRouter
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.NotificationUtil
import com.lyy.keepassa.view.dialog.LoadingDialog
import timber.log.Timber

/**
 * 创建见数据库页面
 */
@Route(path = "/launcher/createDb")
class CreateDbActivity : BaseActivity<ActivityCreateDbBinding>(), View.OnClickListener {
  private var curSetup = 1
  private lateinit var firstFragment: CreateDbFirstFragment
  private var secondFragment: CreateDbSecondFragment? = null
  private lateinit var module: CreateDbModule
  private lateinit var loadingDialog: LoadingDialog

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    module = ViewModelProvider(this).get(CreateDbModule::class.java)
    toolbar.setTitle(R.string.create_db)
    binding.next.setOnClickListener(this)
    binding.up.setOnClickListener(this)
    firstFragment = CreateDbFirstFragment()
    val transaction = supportFragmentManager.beginTransaction()
    transaction.replace(R.id.content, firstFragment)
    transaction.commitNow()
  }

  /**
   * 右 -> 左
   */
  private fun getRlAnim(): Transition {
    return TransitionInflater.from(this)
      .inflateTransition(R.transition.slide_enter)
  }

  /**
   * 左 -> 右
   */
  private fun getLrAnim(): Transition {
    return TransitionInflater.from(this)
      .inflateTransition(R.transition.slide_exit)
  }

  override fun setLayoutId(): Int {
    return R.layout.activity_create_db
  }

  override fun onBackPressed() {
    if (curSetup == 2) {
      upFragment()
    } else {
      finishAfterTransition()
    }
  }

  override fun onClick(v: View?) {
    if (KeepassAUtil.instance.isFastClick()) {
      return
    }
    when (v!!.id) {
      R.id.next -> {
        if (curSetup == 1) {
//          startNextFragment()
          firstFragment.startNext()
        } else { // 完成
          done()
        }
      }
      R.id.up -> upFragment()
    }
  }

  /**
   * 完成信息输入，并创建数据库
   */
  private fun done() {
    // 密码需要重新获取，将密码设置到module中
    secondFragment?.getPass()
    loadingDialog = LoadingDialog(this)
    loadingDialog.show()
    module.createAndOpenDb(this)
      .observe(this, Observer { db ->
        loadingDialog.dismiss()
        if (db == null) {
          HitUtil.toaskShort(getString(R.string.create_db) + getString(R.string.fail))
          return@Observer
        }
        Timber.d("创建数据库成功")
        HitUtil.toaskShort(getString(R.string.hint_db_create_success, module.dbName))
        NotificationUtil.startDbOpenNotify(this)
        Routerfit.create(ActivityRouter::class.java, this).toMainActivity(
          opt = ActivityOptionsCompat.makeSceneTransitionAnimation(this)
        )
        KeepassAUtil.instance.saveLastOpenDbHistory(BaseApp.dbRecord)
      })
  }

  /**
   * 开始设置密码
   */
  fun startNextFragment() {
    if (TextUtils.isEmpty(firstFragment.getDbName())) {
      firstFragment.handleDbNameNull()
      return
    } else if (module.localDbUri == null) {
      firstFragment.showSaveTypeDialog()
      return
    }
    curSetup = 2
    binding.next.setText(R.string.done)
    binding.up.visibility = View.VISIBLE

    if (secondFragment == null) {
      secondFragment = CreateDbSecondFragment()
    }
    /*
     * 重新设置动画：
     * fragment1 （进入）左 -> 右；（退出）左 -> 右
     * fragment2 （进入）右 -> 左；（退出）左 -> 右
     */
    firstFragment.exitTransition = getLrAnim()
    secondFragment!!.enterTransition = getRlAnim()

    val changeBoundsTransition = TransitionInflater.from(this)
//        .inflateTransition(R.transition.changebounds_with_arcmotion)
      .inflateTransition(android.R.transition.move)
    secondFragment!!.sharedElementEnterTransition = changeBoundsTransition

    supportFragmentManager.beginTransaction()
      .replace(R.id.content, secondFragment!!)
      .addSharedElement(firstFragment.getShareElement(), getString(R.string.transition_db_name))
      .commit()
//    changeBg(true)
  }

  /**
   * 返回设置数据库路径
   */
  private fun upFragment() {
    curSetup = 1
    binding.next.setText(R.string.next)
    binding.up.visibility = View.GONE

    /*
     * 重新设置动画：
     * fragment1 （进入）左 -> 右；（退出）右 -> 左
     * fragment2 （进入）右 -> 左；（退出）右 -> 左
     */
    firstFragment.enterTransition = getLrAnim()
    firstFragment.exitTransition = getRlAnim()
    secondFragment!!.exitTransition = getRlAnim()

    val changeBoundsTransition = TransitionInflater.from(this)
//        .inflateTransition(R.transition.changebounds_with_arcmotion)
      .inflateTransition(android.R.transition.move)
    firstFragment.sharedElementEnterTransition = changeBoundsTransition
    supportFragmentManager.beginTransaction()
      .replace(R.id.content, firstFragment)
      .addSharedElement(
        secondFragment!!.getShareElement(), getString(R.string.transition_db_name)
      )
      .commitNow()
//    changeBg(false)
  }

  /**
   * 切换fragment改变背景
   */
  private fun changeBg(toSecondFragment: Boolean) {
    val view = findViewById<Toolbar>(R.id.kpa_toolbar)
    val finalRadius = view.width.coerceAtLeast(view.height)
    val anim = ViewAnimationUtils.createCircularReveal(
      view, if (toSecondFragment) view.right else 0, 0, 0f, finalRadius.toFloat()
    )
    view.setBackgroundResource(
      if (toSecondFragment) R.color.colorPrimary else R.color.white
    )
    anim.duration = resources.getInteger(R.integer.anim_duration_long)
      .toLong()
//    anim.interpolator = AccelerateInterpolator()
    view.visibility = View.VISIBLE
    anim.start()
  }
}