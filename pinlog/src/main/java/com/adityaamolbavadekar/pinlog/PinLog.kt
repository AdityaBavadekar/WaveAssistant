package com.adityaamolbavadekar.pinlog

import android.app.Application
import android.util.Log
import androidx.annotation.NonNull
import com.adityaamolbavadekar.pinlog.database.ApplicationLogDatabaseHelper
import com.adityaamolbavadekar.pinlog.database.ApplicationLogModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.pow

/**
 * Created by Aditya Bavadekar on 18 May 2022
 * */
public object PinLog {

    private var isDevLoggingEnabled = false
    private var BUILD_CONFIG: Class<*>? = null
    private var shouldStoreLogs: Boolean = true
    private var isInitialised: Boolean = false
    private var app: Application? = null
    private var applicationLogDataSource: ApplicationLogDatabaseHelper? = null
    private var pinLoggerService: ExecutorService? = null
    private var LOGGING_STYLE: LoggingStyle? = null
    private var callerPackageName: String = ""
    private var callerVersionName: String = ""
    private var callerVersionCode: String = ""

    fun instance(@NonNull application: Application): Boolean {
        synchronized(PinLog::class.java) {
            return if (this.app == null) {
                PinLog.app = application
                isInitialised = true
                onInitialisation()
                true
            } else {
                logWarning("Could not initialise PinLog as it was previously initialised for $callerPackageName")
                false
            }
        }
    }

    private fun onInitialisation() {
        logInfo("Dev Logging is enabled")
        getAppInfo()
        logInfo("PinLog was initialised successfully for $callerPackageName")
        if (applicationLogDataSource == null) {
            applicationLogDataSource = ApplicationLogDatabaseHelper(app!!.applicationContext)
        }
        collectBuildConfigData()
    }

    private fun getAppInfo() {
        val packageInfo = app!!.packageManager.getPackageInfo(app!!.packageName, 0)
        callerVersionName = packageInfo.versionName
        callerVersionCode = "${packageInfo.versionCode}"
        callerPackageName = packageInfo.packageName
    }

    private fun logError(m: String, e: Exception?) {
        if (isDevLoggingEnabled) Log.e(CLASS_TAG, m, e)
    }

    private fun logWarning(m: String) {
        if (isDevLoggingEnabled) Log.w(CLASS_TAG, m)
    }

    private fun logInfo(m: String) {
        if (isDevLoggingEnabled) Log.i(CLASS_TAG, m)
    }


    /*Public Methods*/

    /*ERROR*/
    fun logE(TAG: String, m: String) {
        if (isDevLoggingEnabled) Log.e(TAG, m)
        storeLog(LogData(TAG, m, null, LogLevel.ERROR))
    }

    fun logE(TAG: String, m: String?, e: Throwable?) {
        if (isDevLoggingEnabled) Log.e(TAG, m, e)
        storeLog(LogData(TAG, m, e, LogLevel.ERROR))
    }

    /*WARN*/
    fun logW(TAG: String, m: String?, e: Throwable?) {
        if (isDevLoggingEnabled) Log.w(TAG, m, e)
        storeLog(LogData(TAG, m, e, LogLevel.WARN))
    }

    fun logW(TAG: String, m: String) {
        if (isDevLoggingEnabled) Log.w(TAG, m)
        storeLog(LogData(TAG, m, null, LogLevel.WARN))
    }

    /*INFO*/
    fun logI(TAG: String, m: String?, e: Throwable?) {
        if (isDevLoggingEnabled) Log.i(TAG, m, e)
        storeLog(LogData(TAG, m, e, LogLevel.INFO))
    }

    fun logI(TAG: String, m: String) {
        if (isDevLoggingEnabled) Log.i(TAG, m)
        storeLog(LogData(TAG, m, null, LogLevel.INFO))
    }

    /*DEBUG*/
    fun logD(TAG: String, m: String?, e: Throwable?) {
        if (isDevLoggingEnabled) Log.d(TAG, m, e)
        storeLog(LogData(TAG, m, e, LogLevel.DEBUG))
    }

    fun logD(TAG: String, m: String) {
        if (isDevLoggingEnabled) Log.d(TAG, m)
        storeLog(LogData(TAG, m, null, LogLevel.DEBUG))
    }

    private fun storeLog(data: LogData) {
        //Filter PinLog's logs as they are intended for Logcat logging
        if (data.TAG == CLASS_TAG) return
        else if (app == null) {
//            if (isDevLoggingEnabled) {
            Log.e(
                CLASS_TAG,
                "Logging method was called before PinLog was initialized. Application Context is null. Please initialise PinLog in Application class before calling any methods",
            )
//            }
            return
        } else if (applicationLogDataSource == null) return


        val log = LOGGING_STYLE!!.getFormattedLogData(
            data.TAG, data.m ?: "", data.dateLong,
            data.level,
            callerVersionName,
            callerVersionCode,
            callerPackageName
        )

        if (pinLoggerService == null) {
            pinLoggerService = Executors.newSingleThreadExecutor()
        }

        val runnable = Runnable {
            try {
                applicationLogDataSource?.insertPinLog(
                    ApplicationLogModel(
                        id = 0,
                        LOG = log,
                        LOG_LEVEL = data.level.LEVEL_INT
                    )
                )
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }
        }
        pinLoggerService?.submit(runnable)
    }

    sealed class LogLevel(val LEVEL_INT: Int, val SHORT_NAME: String) {
        object ERROR : LogLevel(0, "E")
        object WARN : LogLevel(1, "W")
        object INFO : LogLevel(2, "I")
        object DEBUG : LogLevel(3, "D")
    }

    fun getAllPinLogs(): List<ApplicationLogModel> {
        return getAllPinLogs(false)
    }

    fun getAllPinLogs(shouldDeleteExistingLogs: Boolean = true): List<ApplicationLogModel> {
        val logs: MutableList<ApplicationLogModel> = mutableListOf()
        if (applicationLogDataSource != null) {
            logs.addAll(applicationLogDataSource!!.getAllPinLogs())
            if(shouldDeleteExistingLogs) deleteAllPinLogs()
        }
        return logs
    }

    fun getAllPinLogsAsStringList(): List<String> {
        return getAllPinLogsAsStringList(false)
    }

    fun getAllPinLogsAsStringList(shouldDeleteExistingLogs: Boolean = true): List<String> {
        val logs: MutableList<String> = mutableListOf()
        if (applicationLogDataSource != null) {
            logs.addAll(applicationLogDataSource!!.getAllPinLogsAsStringList())
            if(shouldDeleteExistingLogs) deleteAllPinLogs()
        }
        return logs
    }

    fun getAllPinLogsInFile(): File? {
        if (app == null || applicationLogDataSource == null) return null
        val fileName = "${callerPackageName}_LOG_FILE_${Date()}"
        return getAllPinLogsInFile(fileName)
    }

    fun getAllPinLogsInFile(fileName:String): File? {
        if (app == null || applicationLogDataSource == null) return null
        val logsList = getAllPinLogsAsStringList()
        if (logsList.isNotEmpty()){
            val dirPath: String = app!!.applicationContext.getExternalFilesDir(null)!!.absolutePath + "/${callerPackageName}LogFiles"
            try{
                //Create a directory if it doesn't already exist.
                val filePath = File(dirPath)
                if (!filePath.exists()) {
                    if (!filePath.mkdirs()) {
                        logW(
                            CLASS_TAG,
                            "Error occurred while creating directory for log files."
                        )
                        return null
                    }
                }

                //Create a new file with file name
                val logFile: File = File(filePath, fileName)
                val writer = FileWriter(logFile, true)
                val bufferedWriter = BufferedWriter(writer, 4 * 1024.0.pow(2.0).toInt())
                for (logString in logsList){
                    bufferedWriter.write(logString+"\n")
                }

                writer.flush()
                writer.close()

                logI(CLASS_TAG,"The logs are save in a file - ${logFile.absolutePath}")

                return logFile
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
        return null
    }

    fun deleteAllPinLogs() {
        if (applicationLogDataSource == null) return
        else applicationLogDataSource!!.deleteAllPinLogs()
    }


    /**
     * if set to `true` Enables dev logs
     *
     * Uses built-in Android [Log] util to log to Logcat
     * *Defaults to `false`
     *
     * @param enabled Whether Dev Logs are enabled defaults to `false`
     * */
    fun setDevLogging(enabled: Boolean) {
        isDevLoggingEnabled = enabled
    }

    /**
     * Uses built-in room to store logs
     * *Defaults to `true`
     *
     * *Note : Logs are stored for max time-period of seven days of logging.
     * After that they are deleted by [PinLog] service.*
     *
     * @param boolean Whether to store the logs
     *
     * */
    fun setDoStoreLogs(boolean: Boolean) {
        shouldStoreLogs = boolean
    }

    /**
     * Useful if you want build info in pre-logs
     * @param buildConfig The BuildConfig generated by gradle
     * */
    fun setBuildConfigClass(buildConfig: Class<*>) {
        BUILD_CONFIG = buildConfig
    }


    /**
     * Initialises [PinLog]
     *
     * *Note : that the properties will be overwritten by new one if [PinLog] was previously initialised.*
     * @param application Application. Commonly used inside [Application.onCreate] method of your [Application] class
     * @return [Boolean] - If [PinLog] was previously initialised then `false` else `true`.
     *
     * */
    fun initialise(@NonNull application: Application,setDevLoggingEnabled: Boolean): Boolean {
        return initialise(application,setDevLoggingEnabled,true,null)
    }


    /**
     * Initialises [PinLog]
     *
     * *Note : that the properties will be overwritten by new one if [PinLog] was previously initialised.*
     * @param application Application. Commonly used inside [Application.onCreate] method of your [Application] class
     * @return [Boolean] - If [PinLog] was previously initialised then `false` else `true`.
     *
     * */
    fun initialise(@NonNull application: Application,buildConfig: Class<*>?): Boolean {
        return initialise(application,false,true,buildConfig)
    }

    /**
     * Initialises [PinLog]
     *
     * *Note : that the properties will be overwritten by new one if [PinLog] was previously initialised.*
     * @param application Application. Commonly used inside [Application.onCreate] method of your [Application] class
     * @return [Boolean] - If [PinLog] was previously initialised then `false` else `true`.
     *
     * */
    fun initialise(@NonNull application: Application,setDevLoggingEnabled: Boolean,setDoStoreLogs: Boolean,buildConfig: Class<*>?): Boolean {
        if (LOGGING_STYLE == null) {
            LOGGING_STYLE = DefaultApplicationLoggingStyle()
        }
        return instance(application)
    }

    class Initializer {

        /**
         * if set to `true` Enables dev logs
         *
         * Uses built-in Android [Log] util to log to Logcat
         * *Defaults to `false`
         *
         * @param enabled Whether Dev Logs are enabled defaults to `false`
         * */
        fun setDevLogging(enabled: Boolean): Initializer {
            isDevLoggingEnabled = enabled
            return this
        }

        /**
         * Useful if you want build info in pre-logs
         * @param buildConfig The BuildConfig generated by gradle
         * */
        fun setBuildConfigClass(buildConfig: Class<*>): Initializer {
            BUILD_CONFIG = buildConfig
            return this
        }

        /**
         * Uses built-in room to store logs
         * *Defaults to `true`
         *
         * *Note : Logs are stored for max time-period of seven days of logging.
         * After that they are deleted by [PinLog] service.*
         *
         * @param boolean Whether to store the logs
         *
         * */
        fun setDoStoreLogs(boolean: Boolean): Initializer {
            shouldStoreLogs = boolean
            return this
        }

        /**
         * Uses built-in room to store logs
         * *Defaults to [loggingStyle] = [DefaultApplicationLoggingStyle]
         *
         * @param loggingStyle To set this property to custom string log format override [LoggingStyle.getFormattedLogData] method
         *
         * */
        fun setLogFormatting(loggingStyle: LoggingStyle? = null): Initializer {
            loggingStyle?.let {
                LOGGING_STYLE = it
            }
            return this
        }

        /**
         * Initialises [PinLog]
         *
         * *Note : that the properties will be overwritten by new one if [PinLog] was previously initialised.*
         * @param application Application. Commonly used inside [Application.onCreate] method of your [Application] class
         * @return [Boolean] - If [PinLog] was previously initialised then `false` else `true`.
         *
         * */
        fun initialise(@NonNull application: Application): Boolean {
            if (LOGGING_STYLE == null) {
                LOGGING_STYLE = DefaultApplicationLoggingStyle()
            }
            return instance(application)
        }

        /**
         * Initialises [PinLog] in Debug mode that is with all Debug Properties like [isDevLoggingEnabled] to true
         *
         * *Note : that the properties will be overwritten by new one if [PinLog] was previously initialised.*
         * @param application Application. Commonly used inside [Application.onCreate] method of your [Application] class
         * @return [Boolean] - If [PinLog] was previously initialised then `false` else `true`.
         *
         * */
        fun initialiseDebug(@NonNull application: Application): Boolean {
            isDevLoggingEnabled = true
            shouldStoreLogs = true
            if (LOGGING_STYLE == null) {
                LOGGING_STYLE = DefaultApplicationLoggingStyle()
            }
            return instance(application)
        }
    }

    /**
     * Initialises [PinLog] in Debug mode that is with all Debug Properties like [isDevLoggingEnabled] to true
     *
     * *Note : that the properties will be overwritten by new one if [PinLog] was previously initialised.*
     * @param application Application. Commonly used inside [Application.onCreate] method of your [Application] class
     * @return [Boolean] - If [PinLog] was previously initialised then `false` else `true`.
     *
     * */
    fun initialiseDebug(@NonNull application: Application): Boolean {
        isDevLoggingEnabled = true
        shouldStoreLogs = true
        if (LOGGING_STYLE == null) {
            LOGGING_STYLE = DefaultApplicationLoggingStyle()
        }
        return instance(application)
    }

    private fun collectBuildConfigData() {
        val data = JSONObject()
        BUILD_CONFIG?.let { someClass ->
            val fields = someClass.fields
            for (field in fields) {
                try {
                    val value = field[null]

                    value?.let {
                        logI(CLASS_TAG, "BuildConfigData : ${field.name} -> $it ")
                        if (field.type.isArray) {
                            data.put(field.name, JSONArray(listOf(*it as Array<*>)))
                        } else {
                            data.put(field.name, it)
                        }
                    }

                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
            }
        }
        if (data.has("DEBUG")) {
            val isDebug = data.getBoolean("DEBUG")
            if (isDebug) Log.i(CLASS_TAG, "Application Build is of Type Debug")
        }
    }

    const val CLASS_TAG = "PinLog"

}

