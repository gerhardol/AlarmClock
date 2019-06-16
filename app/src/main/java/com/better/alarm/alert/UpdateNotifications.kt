package com.better.alarm.alert

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import com.better.alarm.R
import com.better.alarm.notificationBuilder
import com.better.alarm.presenter.AlarmsListActivity

class UpdateNotifications {
    companion object {
        @JvmStatic
        fun show(context: Context) {
            val notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
            val notification = context.notificationBuilder("UpdateNotifications") {
                setSmallIcon(R.drawable.stat_notify_alarm)
                setContentTitle(context.getString(R.string.updatedNotificationTitle))
                setContentText(context.getString(R.string.updatedNotificationText))
                setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.updatedNotificationText)))
                setContentIntent(
                        PendingIntent.getActivity(
                                context,
                                58,
                                Intent(context, AlarmsListActivity::class.java),
                                PendingIntent.FLAG_UPDATE_CURRENT
                        )
                )
                setAutoCancel(true)
            }
            notificationManager.notify(58, notification)
        }
    }
}