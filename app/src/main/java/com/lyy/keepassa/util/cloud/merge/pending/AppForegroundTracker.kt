package com.lyy.keepassa.util.cloud.merge.pending

import android.app.Activity
import android.app.Application
import android.os.Bundle

class AppForegroundTracker {
  private var startedActivities = 0

  val isForeground: Boolean
    @Synchronized get() = startedActivities > 0

  @Synchronized
  fun onActivityStarted() {
    startedActivities++
  }

  @Synchronized
  fun onActivityStopped() {
    startedActivities = (startedActivities - 1).coerceAtLeast(0)
  }
}

object AppForegroundState {
  private val tracker = AppForegroundTracker()

  val isForeground: Boolean
    get() = tracker.isForeground

  val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
    override fun onActivityStarted(activity: Activity) = tracker.onActivityStarted()
    override fun onActivityStopped(activity: Activity) = tracker.onActivityStopped()
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
  }
}
