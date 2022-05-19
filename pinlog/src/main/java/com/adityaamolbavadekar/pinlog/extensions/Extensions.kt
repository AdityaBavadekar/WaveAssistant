package com.adityaamolbavadekar.pinlog.extensions

import android.app.Application
import com.adityaamolbavadekar.pinlog.PinLog

fun Application.initPinLogger(): Boolean {
    return PinLog.Initializer().initialise(this)
}

fun Application.initPinLoggerInDebugMode(): Boolean {
    return PinLog.initialiseDebug(this)
}