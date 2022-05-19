package com.adityaamolbavadekar.pinlog

data class LogData(
    var TAG: String,
    var m: String?,
    var e: Throwable?,
    var level: PinLog.LogLevel,
    val dateLong : Long = System.currentTimeMillis()
)