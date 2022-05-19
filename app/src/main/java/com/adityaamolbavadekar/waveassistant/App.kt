package com.adityaamolbavadekar.waveassistant

import android.app.Application
import android.content.Intent
import com.adityaamolbavadekar.pinlog.PinLog
import com.hypertrack.hyperlog.HyperLog
import org.acra.ACRA
import org.acra.ACRAConstants
import org.acra.log.ACRALog
import java.util.*
import kotlin.system.exitProcess

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        debug("Hello World")
        HyperLog.initialize(this)
        PinLog.Initializer()
            .setDevLogging(true)
            .setDoStoreLogs(true)
            .setBuildConfigClass(BuildConfig::class.java)
            .initialise(this)


        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            debug("*****************beginning of crash\n\nCrash\nError Info\n${e.message}\n${t.stackTrace}")
            val i = Intent(Intent.ACTION_SEND)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            i.putExtra(Intent.EXTRA_EMAIL, "adityarbavadekar@gmail.com")
            val text =
                "Date : ${Date()}\n\nThread=${t.name}\n\nThreadStack=${t.stackTrace.toList()}\n\nThrowableStack=${e.stackTrace.toList()}\n\nError=$e"
            i.putExtra(Intent.EXTRA_TEXT, text)
            i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            i.type = "text/*"
            startActivity(i)
            exitProcess(0)
        }
    }


}