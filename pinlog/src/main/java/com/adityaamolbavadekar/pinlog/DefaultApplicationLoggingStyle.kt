package com.adityaamolbavadekar.pinlog

import java.util.*

/**
 *
 * ```
 * Example :
 * "Vr[1.0] 05-17 20:04:11.603 18494-18629/? D/PinLog: Hello World from PinLog"
 *
 * ```
 *
 * */
class DefaultApplicationLoggingStyle() : LoggingStyle() {

    override fun getFormattedLogData(
        TAG: String,
        m: String,
        dateLong: Long,
        level: PinLog.LogLevel,
        VERSION_NAME: String,
        VERSION_CODE: String,
        PACKAGE_NAME: String
    ): String {
        return "Vr/[${VERSION_NAME}] " + "${Date(dateLong)}" + "/ " + "${level.SHORT_NAME}/" + "${TAG}:" + m
    }
}