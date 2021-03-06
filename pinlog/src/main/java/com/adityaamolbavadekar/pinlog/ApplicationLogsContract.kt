package com.adityaamolbavadekar.pinlog

import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import com.adityaamolbavadekar.pinlog.ApplicationLogsContract.ApplicationLogEntry.DATABASE_CREATE
import com.adityaamolbavadekar.pinlog.ApplicationLogsContract.ApplicationLogEntry.TABLE_NAME
import com.adityaamolbavadekar.pinlog.PinLog.CLASS_TAG

object ApplicationLogsContract {

    object ApplicationLogEntry : BaseColumns {
        const val DATABASE_VERSION: Int = 1

        //        const val DATABASE_NAME: String = "com.adityaamolbavadekar.pinlog.database.logs.db"
        const val TABLE_NAME: String = "pin_logger_logs"
        const val COLUMN_NAME_ID: String = "_id"
        const val COLUMN_NAME_LOG: String = "logs"
        const val COLUMN_NAME_LOG_LEVEL: String = "log_level"

        const val DATABASE_CREATE = ("CREATE TABLE IF NOT EXISTS "
                + TABLE_NAME
                + " ("
                + COLUMN_NAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NAME_LOG + " TEXT, "
                + COLUMN_NAME_LOG_LEVEL + " INTEGER"
                + ");")
    }


    fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (db == null) {
            return
        }
        try {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            onCreate(db)
            PinLog.logI(
                CLASS_TAG,
                "PinLogTable onUpgrade called. Executing drop_table query to delete old logs."
            )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            PinLog.logE(
                CLASS_TAG,
                "PinLogTable: Exception occurred while executing Database in onUpgrade: $e"
            )
        }
    }

    fun onCreate(db: SQLiteDatabase?) {
        if (db == null) return

        try {
            db.execSQL(DATABASE_CREATE)
            PinLog.logI(
                CLASS_TAG,
                "PinLogTable: Successfully created PinLogs Database"
            )

        } catch (e: Exception) {
            e.printStackTrace()
            PinLog.logE(
                CLASS_TAG,
                "PinLogTable: Exception occurred while executing Database in onCreate: $e"
            )
        }
    }

}

