package com.example.relay.timetable.service

import com.example.relay.timetable.models.GroupTimetableRes
import com.example.relay.timetable.models.MyTimetableRes

interface TimetableInterface {
    fun onGetGroupTimetableSuccess(response: GroupTimetableRes)
    fun onGetGroupTimetableFailure(message: String)
    fun onGetMyTimetableSuccess(response: MyTimetableRes)
    fun onGetMyTimetableFailure(message: String)
    fun onPostMyTimetableSuccess()
    fun onPostMyTimetableFailure(message: String)
}