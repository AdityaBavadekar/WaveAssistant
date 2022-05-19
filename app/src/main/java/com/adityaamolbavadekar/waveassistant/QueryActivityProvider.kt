package com.adityaamolbavadekar.waveassistant

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.UriMatcher
import android.database.ContentObserver
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log

class QueryActivityProvider() : ContentProvider() {

    private var db:SQLiteDatabase? = null

    override fun onCreate(): Boolean {
        val helper = QueryActivityHelper(context)
        db = helper.writableDatabase
        Log.d(javaClass.simpleName,"onCreate [db=${db}]")
        return db == null
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return db?.query(TABLE_NAME,projection,selection,selectionArgs,sortOrder,null,null)
    }

    override fun getType(uri: Uri): String? {
        return MIME_TYPE
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        context?.debug("Insert Request = URI=[$uri]")
        Log.d(javaClass.simpleName,"Insert Request = URI=[$uri]")
        db?.insert(TABLE_NAME,null,values)
        context?.contentResolver?.notifyChange(uri,contentObserver)
        return uri
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val countInt = db?.delete(TABLE_NAME,selection,selectionArgs)!!
        context?.contentResolver?.notifyChange(uri,contentObserver)
        return countInt
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val countInt = db?.update(TABLE_NAME,values,selection,selectionArgs)!!
        context?.contentResolver?.notifyChange(uri,contentObserver)
        return countInt
    }

    private val sUriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply{
        addURI(AUTHORITY, TABLE_NAME, CODE_MATCH)
        addURI(AUTHORITY, TABLE_NAME+"/*", CODE_MATCH)
    }

    companion object{
        const val TAG = "QueryActivityProvider"
        const val TABLE_NAME = "activity"
        const val CODE_MATCH = 500
        const val CODE_NO_MATCH = 404
        const val AUTHORITY = "com.adityaamolbavadekar.waveassistant"
        const val PROVIDER_NAME = "$AUTHORITY/QueryActivityProvider"
        const val PROVIDER_BASE_URL = "content://$PROVIDER_NAME/$TABLE_NAME"
        val CONTENT_URI = Uri.parse(PROVIDER_BASE_URL)
        const val CONTENT_URL = "content://$PROVIDER_NAME/$TABLE_NAME"
        const val MIME_TYPE = "vnd.android.cursor.item/vnd.$AUTHORITY.$TABLE_NAME"
        const val _ID = "_id"
        const val QUERY = "_query"
        const val ANSWER = "_answer"
        const val TIMESTAMP = "_timestamp"

        fun insertQuery(queryResult: QueryResult,contentResolver: ContentResolver): Uri? {
            val values = ContentValues()
            values.put(QUERY,queryResult.rootQueryText)
            values.put(ANSWER,queryResult.answerText)
            values.put(TIMESTAMP,System.currentTimeMillis().toString())
            return contentResolver.insert(CONTENT_URI,values)
        }

    }

    private val contentObserver : ContentObserver? = object : ContentObserver(null) {

        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            this@QueryActivityProvider.context?.debug("onChange")
        }
    }

}