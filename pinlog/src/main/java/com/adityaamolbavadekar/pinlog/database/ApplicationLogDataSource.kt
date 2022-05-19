package com.adityaamolbavadekar.pinlog.database

internal interface ApplicationLogDataSource {
    fun getPinLogsCount(): Int
    fun getPinLogsGroupCount(): Int
    fun insertPinLog(applicationLog: ApplicationLogModel) : Boolean
    fun deleteAllPinLogs()
    fun getPinLogs(group: Int): List<ApplicationLogModel?>?
    fun getAllPinLogs(): List<ApplicationLogModel>
    fun getAllPinLogsAsStringList() : List<String>
    fun deletedExpiredPinLogs(expiryTimeInSeconds: Int)
}