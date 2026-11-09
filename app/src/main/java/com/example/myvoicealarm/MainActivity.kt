package com.example.myvoicealarm

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.File
import java.util.Calendar

class MainActivity : Activity() {
    private lateinit var listBox: LinearLayout
    private var selectedHour = 7
    private var selectedMinute = 0
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordedPath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionsIfNeeded()
        buildScreen()
        refresh()
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = permissions.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 10)
        if (Build.VERSION.SDK_INT >= 31) {
            val am = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (!am.canScheduleExactAlarms()) {
                try { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) } catch (_: Exception) {}
            }
        }
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 20)
            setBackgroundColor(0xFFF5F0FF.toInt())
        }
        val title = TextView(this).apply {
            text = "My Voice Alarm"
            textSize = 27f; setTextColor(0xFF4C1D95.toInt()); gravity = Gravity.CENTER
        }
        root.addView(title, LinearLayout.LayoutParams(-1, 58))
        val subtitle = TextView(this).apply {
            text = "Wake up to your own recorded voice"
            textSize = 14f; gravity = Gravity.CENTER; setTextColor(0xFF6B5B7B.toInt())
        }
        root.addView(subtitle, LinearLayout.LayoutParams(-1, 34))
        val add = Button(this).apply {
            text = "+ Add new alarm"
            setTextColor(-1); setBackgroundColor(0xFF7C3AED.toInt())
            setOnClickListener { addAlarmDialog() }
        }
        root.addView(add, LinearLayout.LayoutParams(-1, 54))
        val scroll = ScrollView(this)
        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 16, 0, 0) }
        scroll.addView(listBox)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val note = TextView(this).apply {
            text = "Tip: Allow microphone, notifications and exact alarms. Disable battery restrictions for reliable alarms."
            textSize = 12f; setTextColor(0xFF6B5B7B.toInt()); setPadding(4, 12, 4, 4)
        }
        root.addView(note)
        setContentView(root)
    }

    private fun addAlarmDialog() {
        recordedPath = ""
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 8, 24, 8) }
        val label = EditText(this).apply { hint = "Alarm name (e.g. Morning reminder)" }
        box.addView(label)
        val time = Button(this).apply { text = "Choose time"; setOnClickListener {
            TimePickerDialog(this@MainActivity, { _, h, m ->
                selectedHour = h; selectedMinute = m
                text = String.format("%02d:%02d", h, m)
            }, selectedHour, selectedMinute, false).show()
        } }
        box.addView(time)
        val record = Button(this).apply { text = "Record my voice"; setOnClickListener {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 10)
                Toast.makeText(this@MainActivity, "Please allow microphone, then tap Record again", Toast.LENGTH_LONG).show()
            } else if (recorder == null) startRecording(this) else stopRecording(this)
        } }
        box.addView(record)
        val daily = CheckBox(this).apply { text = "Repeat every day"; isChecked = true }
        box.addView(daily)
        val dialog = android.app.AlertDialog.Builder(this).setTitle("Create voice alarm")
            .setView(box).setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null).create()
        dialog.setOnShowListener {
            dialog.getButton(-1).setOnClickListener {
                if (recordedPath.isBlank()) {
                    Toast.makeText(this, "Please record your voice first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val a = VoiceAlarm(AlarmStore.nextId(this), selectedHour, selectedMinute,
                    label.text.toString().ifBlank { "My alarm" }, recordedPath, true, daily.isChecked)
                val all = AlarmStore.all(this); all.add(a); AlarmStore.save(this, all)
                AlarmScheduler.schedule(this, a)
                dialog.dismiss(); refresh()
            }
        }
        dialog.show()
    }

    private fun startRecording(button: Button) {
        try {
            val file = File(filesDir, "voice_${System.currentTimeMillis()}.m4a")
            recordingFile = file
            recorder = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare(); start()
            }
            button.text = "Stop recording"
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            recorder?.release(); recorder = null
            Toast.makeText(this, "Could not start recording: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopRecording(button: Button) {
        try {
            recorder?.stop()
            recordedPath = recordingFile?.absolutePath ?: ""
            Toast.makeText(this, "Voice recording saved", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Recording was too short. Please try again.", Toast.LENGTH_SHORT).show()
        } finally {
            recorder?.release(); recorder = null; button.text = "Record again"
        }
    }

    private fun refresh() {
        listBox.removeAllViews()
        val alarms = AlarmStore.all(this)
        if (alarms.isEmpty()) {
            listBox.addView(TextView(this).apply {
                text = "No alarms yet. Tap Add new alarm to get started."
                textSize = 16f; setTextColor(0xFF6B5B7B.toInt()); setPadding(8, 24, 8, 24)
            })
        }
        alarms.forEach { alarm ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(16, 12, 16, 12)
                setBackgroundColor(-1)
            }
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val info = TextView(this).apply {
                text = String.format("%02d:%02d  %s\n%s", alarm.hour, alarm.minute, alarm.label,
                    if (alarm.repeatDaily) "Every day" else "One time")
                textSize = 17f; setTextColor(0xFF3B176D.toInt())
            }
            row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
            val toggle = Switch(this).apply {
                isChecked = alarm.enabled
                setOnCheckedChangeListener { _, checked ->
                    val all = AlarmStore.all(this@MainActivity)
                    val updated = alarm.copy(enabled = checked)
                    AlarmStore.save(this@MainActivity, all.map { if (it.id == alarm.id) updated else it })
                    if (checked) AlarmScheduler.schedule(this@MainActivity, updated) else AlarmScheduler.cancel(this@MainActivity, alarm.id)
                }
            }
            row.addView(toggle)
            card.addView(row)
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val skip = Button(this@MainActivity).apply {
                text = "Skip next day"
                setOnClickListener {
                    val c = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, alarm.hour); set(Calendar.MINUTE, alarm.minute)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
                    }
                    val nextDate = AlarmScheduler.dateKey(c)
                    val updated = alarm.copy(skipDate = nextDate)
                    val all = AlarmStore.all(this@MainActivity)
                    AlarmStore.save(this@MainActivity, all.map { if (it.id == alarm.id) updated else it })
                    AlarmScheduler.schedule(this@MainActivity, updated)
                    Toast.makeText(this@MainActivity, "Next occurrence skipped", Toast.LENGTH_SHORT).show()
                }
            }
            val delete = Button(this@MainActivity).apply {
                text = "Delete"
                setOnClickListener {
                    AlarmScheduler.cancel(this@MainActivity, alarm.id)
                    AlarmStore.save(this@MainActivity, AlarmStore.all(this@MainActivity).filterNot { it.id == alarm.id })
                    refresh()
                }
            }
            actions.addView(skip); actions.addView(delete)
            card.addView(actions)
            val params = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 14 }
            listBox.addView(card, params)
        }
    }
}
