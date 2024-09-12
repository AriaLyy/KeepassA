package com.lyy.keepassa.view

import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.FrameLayout
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ScreenUtils
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.databinding.ActivityTestBinding
import com.lyy.keepassa.util.doClick
import com.lyy.keepassa.widget.toPx
import com.lyy.keepassa.widgets.MainFabSubAction
import com.lyy.keepassa.widgets.arc.FloatingActionMenu

/**
 * @Author laoyuyu
 * @Description
 * @Date 14:51 2024/9/12
 **/
class TestActivity : BaseActivity<ActivityTestBinding>() {
  private var fabMenu: FloatingActionMenu? = null

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    initMenu()
  }

  override fun setLayoutId(): Int {
    return R.layout.activity_test
  }

  // override fun onCreate(savedInstanceState: Bundle?) {
  //   super.onCreate(savedInstanceState)
  //   setContentView(R.layout.activity_test)
  //   initMenu()
  // }

  private fun buildMenuIcon(drawable: Drawable?, onClick: OnClickListener): MainFabSubAction {
    val dp42 = 42.toPx()
    val lp = FrameLayout.LayoutParams(dp42, dp42)
    val menu = MainFabSubAction(this, null)
    menu.layoutParams = lp
    menu.setDrawable(drawable)
    menu.doClick {
      onClick.onClick(menu)
    }
    return menu
  }

  private fun initMenu() {
    val menuKey = buildMenuIcon(
      ResUtil.getSvgIcon(R.drawable.ic_password, R.color.color_FFFFFF)
    ) {
      // Routerfit.create(ActivityRouter::class.java).toCreateEntryActivity(null)
      fabMenu?.close(true)
    }
    val menuGroup = buildMenuIcon(ResUtil.getDrawable(R.drawable.ic_fab_dir)) {
      // Routerfit.create(DialogRouter::class.java)
      //   .showCreateGroupDialog(BaseApp.KDB!!.pm.rootGroup as PwGroupV4)
      fabMenu?.close(true)
    }

    val menuLock =
      buildMenuIcon(ResUtil.getSvgIcon(R.drawable.ic_lock_24px, R.color.color_FFFFFF)) {
        // showQuickUnlockDialog()
      }

    val menuSearch = buildMenuIcon(ResUtil.getSvgIcon(R.drawable.ic_search, R.color.color_FFFFFF)) {
      // showSearchDialog()
      fabMenu?.close(true)
    }
    val fabNew = findViewById<View>(R.id.fabNew)
    fabMenu = FloatingActionMenu.Builder(this)
      .setStateChangeListener(object : FloatingActionMenu.MenuStateChangeListener {
        override fun onMenuOpened(menu: FloatingActionMenu?) {
          // binding.fabNew.rotation = 45f
        }

        override fun onMenuClosed(menu: FloatingActionMenu?) {
          // binding.fabNew.rotation = 0f
        }
      })
      // .setStartAngle(180)
      // .setEndAngle(260)
      .addSubActionView(menuKey)
      .addSubActionView(menuGroup)
      .addSubActionView(menuLock)
      .addSubActionView(menuSearch)
      .setPointInterceptor { mainActionView ->

        val coords = IntArray(2)

        // This method returns a x and y values that can be larger than the dimensions of the device screen.
        mainActionView.getLocationOnScreen(coords)

        // so, we need to deduce the offsets.
        // val activityFrame = Rect()
        // val contentView = binding.root
        // contentView.getWindowVisibleDisplayFrame(activityFrame)
        // coords[0] -= (ScreenUtils.getScreenWidth() - contentView.getMeasuredWidth())
        // coords[1] -= (activityFrame.height() + activityFrame.top
        //   - contentView.getMeasuredHeight()) - BarUtils.getNavBarHeight() - BarUtils.getStatusBarHeight()

        // val localPoint = IntArray(2)
        // mainActionView.getLocationInWindow(localPoint)
        // // Timber.d("localPoint: ${localPoint}")
        // // localPoint[0] -= mainActionView.width / 2
        // localPoint[1] += 4.toPx()
        // BarUtils.getNavBarHeight().let {
        //   if (it != 0){
        //     localPoint[1] -= it / 2
        //   }
        // }

        coords[1] -= BarUtils.getStatusBarHeight()

        coords[0] += mainActionView.measuredWidth / 2
        coords[1] += mainActionView.measuredHeight/2

        // return@setPointInterceptor Point(coords[0], coords[1])
        return@setPointInterceptor Point(coords[0], coords[1])
      }

      .attachTo(fabNew)
      .build()

    fabNew.bringToFront()
    // binding.fabNew.setImageDrawable(module.getAddIcon())
    // binding.fabNew.callback = object : MainFloatActionButton.OnOperateCallback {
    //   override fun onHint(view: MainFloatActionButton) {
    //     fabMenu?.close(true)
    //   }
    // }
  }
}