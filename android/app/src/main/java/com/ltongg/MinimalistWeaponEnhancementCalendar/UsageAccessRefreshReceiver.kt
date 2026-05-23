package com.ltongg.MinimalistWeaponEnhancementCalendar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.concurrent.Executors

class UsageAccessRefreshReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (UsageAccessScheduler.isEnabled(context)) {
      val slotLabel = UsageAccessScheduler.alarmSlotLabelFromIntent(intent)
      val minute = UsageAccessScheduler.alarmMinuteFromIntent(intent)
      UsageAccessScheduler.markAlarmReceived(context, slotLabel, minute)
      val pendingResult = goAsync()
      executor.execute {
        try {
          UsageAccessScheduler.refreshUsageStats(context, UsageAccessScheduler.refreshReasonForSlot(slotLabel))
          UsageAccessScheduler.scheduleDailyRefresh(context)
        } finally {
          pendingResult.finish()
        }
      }
    }
  }

  companion object {
    private val executor = Executors.newSingleThreadExecutor()
  }
}
