package com.lyy.keepassa.view.menu

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.MenuInflater
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentActivity
import com.arialyy.frame.util.ReflectionUtil
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.event.DelAttrStrEvent
import com.lyy.keepassa.view.create.CreateCustomStrDialog
import com.lyy.keepassa.widget.expand.AttrStrItemView
import org.greenrobot.eventbus.EventBus

/**
 * 创建或编辑条目自定义属性悬浮框
 */
@SuppressLint("RestrictedApi")
class EntryCreateStrPopMenu(
  private val context: FragmentActivity,
  view: AttrStrItemView,
  private val key: String,
  private val str: ProtectedString
) : IPopMenu {
  private val popup: PopupMenu = PopupMenu(context, view, Gravity.END)
  private val help: MenuPopupHelper

  init {
    val inflater: MenuInflater = popup.menuInflater
    inflater.inflate(R.menu.pop_create_entry_summary, popup.menu)

    // 以下代码为强制显示icon
    val mPopup = ReflectionUtil.getField(PopupMenu::class.java, "mPopup")
    mPopup.isAccessible = true
    help = mPopup.get(popup) as MenuPopupHelper
    help.setForceShowIcon(true)
    popup.setOnMenuItemClickListener { item ->
      when (item.itemId) {
        R.id.del -> {
          EventBus.getDefault().post(DelAttrStrEvent(key, str))
        }
        R.id.edit -> {
          val dialog = CreateCustomStrDialog(true, view)
          dialog.setData(key, str)
          dialog.show(context.supportFragmentManager, "create_custom_dialog")
        }
      }
      popup.dismiss()
      true
    }

  }

  public fun show() {
    help.show()
  }
}