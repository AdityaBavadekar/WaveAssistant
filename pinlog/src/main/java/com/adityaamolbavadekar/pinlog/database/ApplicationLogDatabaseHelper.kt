package com.adityaamolbavadekar.pinlog.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.adityaamolbavadekar.pinlog.PinLog
import com.adityaamolbavadekar.pinlog.PinLog.CLASS_TAG
import com.adityaamolbavadekar.pinlog.ApplicationLogsContract
import com.adityaamolbavadekar.pinlog.ApplicationLogsContract.ApplicationLogEntry.COLUMN_NAME_ID
import com.adityaamolbavadekar.pinlog.ApplicationLogsContract.ApplicationLogEntry.COLUMN_NAME_LOG
import com.adityaamolbavadekar.pinlog.ApplicationLogsContract.ApplicationLogEntry.COLUMN_NAME_LOG_LEVEL
import com.adityaamolbavadekar.pinlog.ApplicationLogsContract.ApplicationLogEntry.DATABASE_VERSION
import com.adityaamolbavadekar.pinlog.ApplicationLogsContract.ApplicationLogEntry.TABLE_NAME

class ApplicationLogDatabaseHelper(c: Context) : SQLiteOpenHelper(
    c,
    TABLE_NAME,
    null,
    DATABASE_VERSION
), ApplicationLogDataSource {

    private var context: Context = c
    private var database: SQLiteDatabase? = null

    private fun initializePinLogsDatabase() {
        if (database == null) {
            database = this.writableDatabase
        }
    }

    /*START [SQLiteOpenHelper]*/
    override fun onCreate(db: SQLiteDatabase?) {
        ApplicationLogsContract.onCreate(db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        ApplicationLogsContract.onUpgrade(db, oldVersion, newVersion)
    }

    /*END [SQLiteOpenHelper]*/

    override fun getPinLogsCount(): Int {
        // Initialize SQLiteDatabase if it is null
        initializePinLogsDatabase()
        var count = 0
        try {
            if (database != null) {
                count = DatabaseUtils.queryNumEntries(database, TABLE_NAME).toInt()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            PinLog.logE(
                CLASS_TAG,
                "PinLogTable: Exception occurred in Database while executing getCount: $e"
            )
        }
        return count
    }

    override fun getPinLogsGroupCount(): Int {
        val c = getPinLogsCount()
        return if (c <= 0) {
            0
        } else {
            (c / 5000).toInt()
        }
    }

    override fun insertPinLog(applicationLog: ApplicationLogModel): Boolean {
        val errorCode: Long = -1
        var rowId: Long = -1
        database?.let { db ->
            val contentValues = ContentValues()
            contentValues.put(COLUMN_NAME_LOG, applicationLog.LOG)
            contentValues.put(COLUMN_NAME_LOG_LEVEL, applicationLog.LOG_LEVEL)

            try {
                rowId = db.insert(TABLE_NAME, null, contentValues)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                PinLog.logE(
                    CLASS_TAG,
                    "PinLogTable: Exception occurred in Database while executing insertPinLog: $e"
                )
            }
        }
        return (rowId != errorCode)
    }

    override fun deleteAllPinLogs() {
        database?.let { db ->

            try {
                db.delete(TABLE_NAME, null, null)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                PinLog.logE(
                    CLASS_TAG,
                    "PinLogTable: Exception occurred in Database while executing deleteAllPinLogs: $e"
                )
            }

        }
    }

    override fun getPinLogs(group: Int): List<ApplicationLogModel> {
        return emptyList()//Not implemented
    }

    private fun getCursor(db: SQLiteDatabase): Cursor? {
        return db.query(
            TABLE_NAME,
            arrayOf(
                COLUMN_NAME_ID,//0
                COLUMN_NAME_LOG,//1
                COLUMN_NAME_LOG_LEVEL//2
            ),
            null,
            null,
            null,
            null,
            null,
            null
        )
    }

    override fun getAllPinLogs(): List<ApplicationLogModel> {
        val applicationLogsList: MutableList<ApplicationLogModel> = mutableListOf()
        database?.let { db ->
            val c = getCursor(db)

            if (c == null || c.isClosed) return emptyList()
            else try {
                if (c.moveToFirst()) {
                    do {
                        if (c.isClosed) {
                            break
                        }
                        val id: Int = c.getInt(0)
                        val log: String = c.getString(1)
                        val level: Int = c.getInt(2)
                        val pinLog = ApplicationLogModel(id, log, level)
                        applicationLogsList.add(pinLog)

                    } while (c.moveToNext())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                PinLog.logE(
                    CLASS_TAG,
                    "PinLogTable: Exception occurred in Database while executing getPinLogs: $e"
                )
            }
            c.close()
            return applicationLogsList.toList()

        }
        return applicationLogsList.toList()
    }

    override fun getAllPinLogsAsStringList(): List<String> {
        val pinLogsList: MutableList<String> = mutableListOf()
        database?.let { db ->
            val c = getCursor(db)

            if (c == null || c.isClosed) return emptyList()
            else try {
                if (c.moveToFirst()) {
                    do {
                        if (c.isClosed) {
                            break
                        }
                        val log: String = c.getString(1)
                        pinLogsList.add(log)
                    } while (c.moveToNext())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                PinLog.logE(
                    CLASS_TAG,
                    "PinLogTable: Exception occurred in Database while executing getAllPinLogsAsStringList: $e"
                )
            }
            c.close()
            return pinLogsList.toList()

        }
        return pinLogsList.toList()
    }

    override fun deletedExpiredPinLogs(expiryTimeInSeconds: Int) {
        if (database == null) {
            return
        }
        //Not yet implemented
    }

    init {
        initializePinLogsDatabase()
    }

}