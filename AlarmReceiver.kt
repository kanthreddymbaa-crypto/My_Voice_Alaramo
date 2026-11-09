package com.example.myvoicealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("alarmId", -1)
        val alarms = AlarmStore.all(context)
        val alarm = alarms.firstOrNull { it.id == id } ?: return
        val today = AlarmScheduler.dateKey(Calendar.getInstance())
        if (!alarm.enabled || alarm.skipDate == today || alarm.lastDate == today) {
            AlarmScheduler.schedule(context, alarm.copy(lastDate = if (alarm.lastDate == today) alarm.lastDate else ""))
            return
        }
        val updated = alarm.copy(lastDate = today)
        AlarmStore.save(context, alarms.map { if (it.id == id) updated else it })
        val service = Intent(context, AlarmPlaybackService::class.java)
            .putExtra("audioPath", alarm.audioPath).putExtra("label", alarm.label)
            .putExtra("alarmId", alarm.id)
        ContextCompat.startForegroundService(context, service)
        if (alarm.repeatDaily) AlarmScheduler.schedule(context, updated)
    }
}
