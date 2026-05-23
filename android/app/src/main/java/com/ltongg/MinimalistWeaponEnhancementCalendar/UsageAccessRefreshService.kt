package com.ltongg.MinimalistWeaponEnhancementCalendar

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import java.util.concurrent.Executors

class UsageAccessRefreshService : Service() {
  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val slotLabel = UsageAccessScheduler.alarmSlotLabelFromIntent(intent)
    val minute = UsageAccessScheduler.alarmMinuteFromIntent(intent)
    UsageAccessScheduler.markAlarmReceived(this, slotLabel, minute)
    startForeground(NOTIFICATION_ID, buildNotification())

    executor.execute {
      try {
        if (UsageAccessScheduler.isEnabled(this)) {
          UsageAccessScheduler.refreshUsageStats(this, UsageAccessScheduler.refreshReasonForSlot(slotLabel))
          UsageAccessScheduler.scheduleDailyRefresh(this)
        }
      } finally {
        stopForegroundCompat()
        stopSelf(startId)
      }
    }

    return START_REDELIVER_INTENT
  }

  private fun buildNotification(): Notification {
    ensureNotificationChannel()
    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Notification.Builder(this, CHANNEL_ID)
    } else {
      Notification.Builder(this)
    }
    return builder
      .setSmallIcon(applicationInfo.icon)
      .setContentTitle("正在同步使用记录")
      .setContentText("正在读取最近三天的黑名单应用使用记录")
      .setOngoing(true)
      .setShowWhen(false)
      .build()
  }

  private fun ensureNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
    if (existing != null) return
    val channel = NotificationChannel(
      CHANNEL_ID,
      "使用记录同步",
      NotificationManager.IMPORTANCE_LOW
    ).apply {
      description = "自动同步黑名单应用使用记录时显示"
      setShowBadge(false)
    }
    notificationManager.createNotificationChannel(channel)
  }

  private fun stopForegroundCompat() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      stopForeground(STOP_FOREGROUND_REMOVE)
    } else {
      @Suppress("DEPRECATION")
      stopForeground(true)
    }
  }

  companion object {
    private const val CHANNEL_ID = "usage_access_refresh"
    private const val NOTIFICATION_ID = 235559
    private val executor = Executors.newSingleThreadExecutor()
  }
}
