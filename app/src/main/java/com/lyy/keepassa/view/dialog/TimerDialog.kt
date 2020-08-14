package com.lyy.keepassa.view.dialog

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.FrameLayout
import android.widget.TimePicker
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.arialyy.frame.base.BaseDialog
import com.arialyy.frame.util.DpUtils
import com.lyy.keepassa.R
import com.lyy.keepassa.databinding.DialogTimerBinding
import com.lyy.keepassa.event.TimeEvent
import org.greenrobot.eventbus.EventBus

/**
 * 时间选择器
 */
class TimerDialog : BaseDialog<DialogTimerBinding>(), View.OnClickListener {
  private lateinit var vpAdapter: VpAdapter

  override fun setLayoutId(): Int {
    return R.layout.dialog_timer
  }

  override fun initData() {
    super.initData()
    binding.tabLayout.setupWithViewPager(binding.vp)
    vpAdapter = VpAdapter(
        listOf(getString(R.string.date), getString(R.string.time)),
        listOf(DatePickerFragment(), TimerPickerFragment()),
        childFragmentManager,
        FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
    )
    binding.vp.adapter = vpAdapter
    binding.cancel.setOnClickListener(this)
    binding.save.setOnClickListener(this)

  }

  override fun onClick(v: View?) {
    when (v!!.id) {
      R.id.cancel -> dismiss()
      R.id.save -> {
        val date = (vpAdapter.getItem(0) as DatePickerFragment).datePicker
        val time = (vpAdapter.getItem(1) as TimerPickerFragment).timerPicker
        val event: TimeEvent
        event = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
          TimeEvent(
              date.year,
              date.month,
              date.dayOfMonth,
              time.currentHour,
              time.currentMinute
          )
        } else {
          TimeEvent(date.year, date.month, date.dayOfMonth, time.hour, time.minute)
        }
        EventBus.getDefault()
            .post(event)
        dismiss()
      }
    }
  }

  override fun onStart() {
    super.onStart()
    dialog?.window?.setLayout(DpUtils.dp2px(280), DpUtils.dp2px(530))
  }

  private class VpAdapter(
    private val titles: List<String>,
    private val fragments: List<Fragment>,
    fm: FragmentManager,
    state: Int
  ) : FragmentPagerAdapter(fm, state) {

    override fun getPageTitle(position: Int): CharSequence? {
      return titles[position]
    }

    override fun getItem(position: Int): Fragment {
      return fragments[position]
    }

    override fun getCount(): Int {
      return fragments.size
    }
  }

  class DatePickerFragment : Fragment() {
    lateinit var datePicker: DatePicker
    override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
    ): View? {
      datePicker = DatePicker(context!!)
      datePicker.layoutParams = FrameLayout.LayoutParams(
          FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
      )
      return datePicker
    }
  }

  class TimerPickerFragment : Fragment() {
    lateinit var timerPicker: TimePicker
    override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
    ): View? {
      timerPicker = TimePicker(context)
      timerPicker.layoutParams = FrameLayout.LayoutParams(
          FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
      )

      return timerPicker
    }

  }

}