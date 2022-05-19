package com.adityaamolbavadekar.pinlog

import android.content.Context


/**
 * Created by Aditya Bavadekar on 18 May 2022
 *
 *
 * Set this property in [PinLog.Initializer.setLogFormatting]. Override the [LoggingStyle.getFormattedLogData] method to implement custom logging string.
 *
 * *Default implementation is [DefaultApplicationLoggingStyle]*
 *
 * */
abstract class LoggingStyle {

    private var context:Context? =null

    constructor(c: Context?):this(){
        context = c
    }

    constructor()

    /**
     * Called before storing log to Database.
     *
     * *Default implementation is [DefaultApplicationLoggingStyle.getFormattedLogData]*
     * */
    abstract fun getFormattedLogData(
        TAG: String,
        m: String,
        dateLong: Long,
        level: PinLog.LogLevel,
        VERSION_NAME: String,
        VERSION_CODE: String,
        PACKAGE_NAME: String
    ): String

}