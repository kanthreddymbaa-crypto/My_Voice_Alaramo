package com.example.myvoicealarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder

class AlarmPlaybackService : Service() {
    private var player: MediaPlayer? = null
    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel("voice_alarm", "Voice alarms", NotificationManager.IMPORTANCE_HIGH))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val label = intent?.getStringExtra("label") ?: "My Voice Alarm"
        val path = intent?.getStringExtra("audioPath") ?: ""
        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, "voice_alarm").setContentTitle("Alarm: $label")
                .setContentText("Your recorded voice alarm is playing")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setOngoing(true).build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this).setContentTitle("Alarm: $label")
                .setContentText("Your recorded voice alarm is playing")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).build()
        }
        startForeground(101, notification)
        try {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener { stopSelf() }
                setOnErrorListener { mp, _, _ -> mp.release(); stopSelf(); true }
                prepare()
                start()
            }
        } catch (_: Exception) { stopSelf() }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
