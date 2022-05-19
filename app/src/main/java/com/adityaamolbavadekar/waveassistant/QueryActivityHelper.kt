package com.adityaamolbavadekar.waveassistant

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class QueryActivityHelper(private val context: Context?) : SQLiteOpenHelper(context,"query_activity_database",null,1) {


    override fun onCreate(db: SQLiteDatabase?) {
        val sqlString = "CREATE TABLE activity(_id INTEGER PRIMARY KEY AUTOINCREMENT,_query TEXT,_answer TEXT, _timestamp TEXT) "
        db?.execSQL(sqlString)
        //Debug Insert
        db?.execSQL("INSERT INTO activity(_query,_answer, _timestamp) VALUES('Example Debug Query 1','Example Debug Answer 1 by Wave Assistant','16 May') ")
        db?.execSQL("INSERT INTO activity(_query,_answer, _timestamp) VALUES('Example Debug Query 2','Example Debug Answer 2 by Wave Assistant','17 May') ")

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }



}