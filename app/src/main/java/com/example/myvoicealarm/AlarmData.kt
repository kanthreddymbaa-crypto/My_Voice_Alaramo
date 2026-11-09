package com.example.myvoicealarm

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class VoiceAlarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val label: String,
    val audioPath: String,
    val enabled: Boolean = true,
    val repeatDaily: Boolean = true,
    val skipDate: String = "",
    val lastDate: String = ""
)

object AlarmStore {
    private const val PREF = "voice_alarm_store"
    private const val KEY = "alarms"

    fun all(context: Context): MutableList<VoiceAlarm> {
        val raw = context.getSharedPreferences(PREF, 0).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<VoiceAlarm>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(VoiceAlarm(
                o.getInt("id"), o.getInt("hour"), o.getInt("minute"),
                o.optString("label"), o.optString("audioPath"),
                o.optBoolean("enabled", true), o.optBoolean("repeatDaily", true),
                o.optString("skipDate"), o.optString("lastDate")
            ))
        }
        return list
    }

    fun save(context: Context, alarms: List<VoiceAlarm>) {
        val arr = JSONArray()
        alarms.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id); put("hour", a.hour); put("minute", a.minute)
                put("label", a.label); put("audioPath", a.audioPath)
                put("enabled", a.enabled); put("repeatDaily", a.repeatDaily)
                put("skipDate", a.skipDate); put("lastDate", a.lastDate)
            })
        }
        context.getSharedPreferences(PREF, 0).edit().putString(KEY, arr.toString()).apply()
    }

    fun nextId(context: Context) = (all(context).maxOfOrNull { it.id } ?: 0) + 1
}
