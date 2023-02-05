package com.example.relay.timetable.models

data class MemSchedule(
    val timeTableIdx:Long,
    val day:Long,
    val start:String,
    val end:String,
    val goal:Long,
    val goalType:String
)