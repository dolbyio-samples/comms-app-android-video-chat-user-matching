package com.example.interactivitydemo.models

import android.util.Log
import java.util.*

private const val CONFERENCE_TAG = "ConferenceModel"

class ConferenceModel(private val alias: String, val id: String, val dolbyVoice: Boolean, private val duration: Int, val ownerName: String, private val role: String) {

    fun getDurationInMinutes(): String { // string operations to return prettified version of duration of conf (originally in milliseconds)
        val minutes = maxOf((duration/1000)/60, 1)

        Log.d(CONFERENCE_TAG, "$minutes")
        when (minutes) {
            in 0..59 -> {
                val stringRep = if (minutes < 10) "0${minutes}" else "$minutes"
                return "00:$stringRep"
            }
            in 60..180 -> { // 180 is max minutes
                val hours = minutes/60
                val leftoverMinutes = if (minutes%60 < 10) "0${minutes%60}" else "${minutes%60}"
                return "0${hours}:${leftoverMinutes}"
            }
        }
        return "0:01"
    }

    fun getAlias(): String { // truncates alias if too long
        return if (alias.length < 20) alias else alias.substring(0, 20).plus("...")
    }

    fun getAliasFull(): String {
        return alias
    }

    fun getOwnerText(): String {
        return "$ownerName, ${role.toLowerCase(Locale.getDefault())}"
    }

    override fun toString(): String {
        return "alias: ${getAlias()}, id: ${id}, ownerName: ${ownerName}"
    }


}