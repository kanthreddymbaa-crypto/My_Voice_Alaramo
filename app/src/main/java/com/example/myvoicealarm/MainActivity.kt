
package com.example.myvoicealarm

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaRecorder
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

        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), 10)
        }

        if (Build.VERSION.SDK_INT >= 31) {
            val am = getSystemService(ALARM_SERVICE) as android.app.AlarmManager

            if (!am.canScheduleExactAlarms()) {
                try {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    )
                } catch (_: Exception) {
                }
            }
        }
    }

    // MAIN SCREEN

    private fun buildScreen() {

        window.statusBarColor = Color.rgb(248, 243, 255)
        window.navigationBarColor = Color.rgb(248, 243, 255)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(12))
            setBackgroundColor(Color.rgb(248, 243, 255))
        }

        val title = TextView(this).apply {
            text = "My Voice Alarm"
            textSize = 29f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.rgb(76, 29, 149))
            gravity = Gravity.CENTER
        }

        root.addView(title, LinearLayout.LayoutParams(-1, dp(48)))

        val subtitle = TextView(this).apply {
            text = "Wake up to your own recorded voice"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(110, 91, 132))
        }

        root.addView(subtitle, LinearLayout.LayoutParams(-1, dp(30)))

        val add = TextView(this).apply {
            text = "+   ADD NEW ALARM"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = roundedBg(Color.rgb(124, 58, 237), dp(16))
            elevation = dp(4).toFloat()
            isClickable = true
            isFocusable = true
            setOnClickListener {
                addAlarmDialog()
            }
        }

        val addParams = LinearLayout.LayoutParams(-1, dp(54))
        addParams.topMargin = dp(14)
        addParams.bottomMargin = dp(12)
        root.addView(add, addParams)

        val scroll = ScrollView(this).apply {
            clipToPadding = false
            isFillViewport = true
        }

        listBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(16))
        }

        scroll.addView(listBox)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val note = TextView(this).apply {
            text = "💜 Allow microphone, notifications and exact alarms for reliable reminders."
            textSize = 12f
            setTextColor(Color.rgb(103, 80, 130))
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(12), dp(8), dp(8))
        }

        root.addView(note)

        setContentView(root)
    }

    // ADD ALARM

    private fun addAlarmDialog() {

        recordedPath = ""

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), dp(8))
        }

        val label = EditText(this).apply {
            hint = "Alarm name (e.g. Morning reminder)"
        }

        box.addView(label)

        val time = Button(this).apply {
            text = String.format(
                "%02d:%02d",
                selectedHour,
                selectedMinute
            )

            setOnClickListener {
                TimePickerDialog(
                    this@MainActivity,
                    { _, h, m ->
                        selectedHour = h
                        selectedMinute = m
                        text = String.format("%02d:%02d", h, m)
                    },
                    selectedHour,
                    selectedMinute,
                    false
                ).show()
            }
        }

        box.addView(time)

        val record = Button(this).apply {
            text = "Record my voice"

            setOnClickListener {

                if (checkSelfPermission(
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        10
                    )

                    Toast.makeText(
                        this@MainActivity,
                        "Allow microphone and tap Record again",
                        Toast.LENGTH_LONG
                    ).show()

                } else if (recorder == null) {
                    startRecording(this)
                } else {
                    stopRecording(this)
                }
            }
        }

        box.addView(record)

        val daily = CheckBox(this).apply {
            text = "Repeat every day"
            isChecked = true
        }

        box.addView(daily)

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Create voice alarm")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()

        dialog.setOnShowListener {

            dialog.getButton(-1).setOnClickListener {

                if (recordedPath.isBlank()) {
                    Toast.makeText(
                        this,
                        "Please record your voice first",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val alarm = VoiceAlarm(
                    AlarmStore.nextId(this),
                    selectedHour,
                    selectedMinute,
                    label.text.toString().ifBlank { "My alarm" },
                    recordedPath,
                    true,
                    daily.isChecked
                )

                val all = AlarmStore.all(this)
                all.add(alarm)

                AlarmStore.save(this, all)
                AlarmScheduler.schedule(this, alarm)

                dialog.dismiss()
                refresh()
            }
        }

        dialog.show()
    }

    // VOICE RECORDING

    private fun startRecording(button: Button) {

        try {
            val file = File(
                filesDir,
                "voice_${System.currentTimeMillis()}.m4a"
            )

            recordingFile = file

            recorder = if (Build.VERSION.SDK_INT >= 31) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            button.text = "Stop recording"

            Toast.makeText(
                this,
                "Recording started",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {

            recorder?.release()
            recorder = null

            Toast.makeText(
                this,
                "Could not start recording: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun stopRecording(button: Button) {

        try {
            recorder?.stop()

            recordedPath = recordingFile?.absolutePath ?: ""

            Toast.makeText(
                this,
                "Voice recording saved",
                Toast.LENGTH_SHORT
            ).show()

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Recording was too short. Please try again.",
                Toast.LENGTH_SHORT
            ).show()

        } finally {
            recorder?.release()
            recorder = null
            button.text = "Record again"
        }
    }

    // REFRESH ALARM LIST

    private fun refresh() {

        listBox.removeAllViews()

        val alarms = AlarmStore.all(this)

        if (alarms.isEmpty()) {

            val empty = TextView(this).apply {
                text = "⏰\n\nNo alarms yet!\n\nTap Add New Alarm to get started."
                textSize = 17f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(110, 91, 132))
                setPadding(
                    dp(20),
                    dp(45),
                    dp(20),
                    dp(45)
                )
            }

            listBox.addView(empty)
        }

        alarms.forEach { alarm ->

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    dp(18),
                    dp(16),
                    dp(18),
                    dp(14)
                )
                background = roundedBg(Color.WHITE, dp(18))
                elevation = dp(3).toFloat()
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val info = TextView(this).apply {
                text = String.format(
                    "%02d:%02d\n%s\n%s",
                    alarm.hour,
                    alarm.minute,
                    alarm.label,
                    if (alarm.repeatDaily) "🔁 Every day" else "One time"
                )

                textSize = 17f
                setTextColor(Color.rgb(59, 23, 109))
                setLineSpacing(dp(3).toFloat(), 1f)
            }

            row.addView(
                info,
                LinearLayout.LayoutParams(0, -2, 1f)
            )

            val toggle = Switch(this).apply {

                isChecked = alarm.enabled

                setOnCheckedChangeListener { _, checked ->

                    val all = AlarmStore.all(this@MainActivity)
                    val updated = alarm.copy(enabled = checked)

                    AlarmStore.save(
                        this@MainActivity,
                        all.map {
                            if (it.id == alarm.id) updated else it
                        }
                    )

                    if (checked) {
                        AlarmScheduler.schedule(
                            this@MainActivity,
                            updated
                        )
                    } else {
                        AlarmScheduler.cancel(
                            this@MainActivity,
                            alarm.id
                        )
                    }
                }
            }

            row.addView(toggle)
            card.addView(row)

            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val skip = TextView(this).apply {

                text = "SKIP NEXT DAY"
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(91, 33, 182))
                background = roundedBg(
                    Color.rgb(237, 229, 255),
                    dp(10)
                )

                setPadding(
                    dp(14),
                    dp(12),
                    dp(14),
                    dp(12)
                )

                setOnClickListener {

                    val c = Calendar.getInstance().apply {

                        set(Calendar.HOUR_OF_DAY, alarm.hour)
                        set(Calendar.MINUTE, alarm.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)

                        if (timeInMillis <= System.currentTimeMillis()) {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }

                    val nextDate = AlarmScheduler.dateKey(c)
                    val updated = alarm.copy(skipDate = nextDate)
                    val all = AlarmStore.all(this@MainActivity)

                    AlarmStore.save(
                        this@MainActivity,
                        all.map {
                            if (it.id == alarm.id) updated else it
                        }
                    )

                    AlarmScheduler.schedule(
                        this@MainActivity,
                        updated
                    )

                    Toast.makeText(
                        this@MainActivity,
                        "Next occurrence skipped",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            val delete = TextView(this).apply {

                text = "DELETE"
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = roundedBg(
                    Color.rgb(124, 58, 237),
                    dp(10)
                )

                setPadding(
                    dp(20),
                    dp(12),
                    dp(20),
                    dp(12)
                )

                setOnClickListener {

                    AlarmScheduler.cancel(
                        this@MainActivity,
                        alarm.id
                    )

                    AlarmStore.save(
                        this@MainActivity,
                        AlarmStore.all(this@MainActivity)
                            .filterNot { it.id == alarm.id }
                    )

                    refresh()
                }
            }

            val skipParams = LinearLayout.LayoutParams(-2, -2)
            skipParams.topMargin = dp(14)
            skipParams.marginEnd = dp(10)

            actions.addView(skip, skipParams)

            val deleteParams = LinearLayout.LayoutParams(-2, -2)
            deleteParams.topMargin = dp(14)

            actions.addView(delete, deleteParams)

            card.addView(actions)

            val cardParams = LinearLayout.LayoutParams(-1, -2)
            cardParams.bottomMargin = dp(14)

            listBox.addView(card, cardParams)
        }
    }

    // DESIGN HELPERS

    private fun dp(value: Int): Int {
        return (
            value * resources.displayMetrics.density
        ).toInt()
    }

    private fun roundedBg(
        color: Int,
        radius: Int
    ): GradientDrawable {

        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }
    }
}
