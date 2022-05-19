package com.adityaamolbavadekar.waveassistant

import android.content.ContentResolver
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hypertrack.hyperlog.HyperLog
import com.adityaamolbavadekar.pinlog.PinLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private var workerInstance: TextQuery.Worker? = null
    private var analyserInstance: TextQuery.Analyser? = null
    private var logs: MutableLiveData<List<String>> = MutableLiveData(listOf())

    fun initWorker(worker: TextQuery.Worker, contentResolver: ContentResolver) {
        this.workerInstance = worker
        CoroutineScope(Dispatchers.Default).launch {
            workerInstance!!.initialise()
            workerInstance!!.viewModel = this@MainViewModel
            workerInstance!!.resolver = contentResolver
        }
    }

    fun initAnalyser(analyser: TextQuery.Analyser?) {
        this.analyserInstance = analyser
    }

    fun workOn(queryResult: QueryResult) {
        CoroutineScope(Dispatchers.IO).launch {
            workerInstance?.workOn(queryResult)
        }
    }

    fun analyse(queryText: String, listener: TextQuery.Analyser.OnQueryAnalysisCompleteListener) {
        CoroutineScope(Dispatchers.Default).launch {
            analyserInstance?.analyse(queryText, listener)
        }
    }

    fun load(listener: TextQuery.Loader.OnLoadCompleteListener, context: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            TextQuery.Loader().load(listener, context)
        }
    }

    fun startLoadingLogs() {
        CoroutineScope(Dispatchers.IO).launch {
//            val list = HyperLog.getDeviceLogsAsStringList(false)
            val list = PinLog.getAllPinLogsAsStringList()
            logs.postValue(list)
        }
    }

    fun getLogs(): LiveData<List<String>> = logs

    fun insertIntoQueryActivity(result: QueryResult, resolver: ContentResolver) {
        CoroutineScope(Dispatchers.IO).launch {
            val uri = QueryActivityProvider.insertQuery(result, resolver)
            if (BuildConfig.DEBUG){
                Log.i("QueryActivityProvider","Query Activity Inserted [${uri}]")
            }
        }
    }


}
