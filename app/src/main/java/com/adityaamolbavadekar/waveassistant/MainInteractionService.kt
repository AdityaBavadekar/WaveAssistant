package com.adityaamolbavadekar.waveassistant

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.voice.VoiceInteractionService
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.adityaamolbavadekar.waveassistant.Assistant.Notifications
import java.util.*
import kotlin.random.Random

class MainInteractionService : VoiceInteractionService() {

    private lateinit var recognizer: SpeechRecognizer

    override fun onReady() {
        super.onReady()
        debug("onReady")
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannels(nm)
        }
        if (!prefs().getBoolean(Assistant.SETUP_COMPLETED, false)) {
            showSetupNotification(nm)
        }
    }


    override fun onCreate() {
        super.onCreate()
/*

        val serviceComponent = Settings.Secure.getString(
            contentResolver, VOICE_RECOGNITION_SERVICE
        )

        val componentName = if (TextUtils.isEmpty(serviceComponent)) {
            debug("no selected voice recognition service [VOICE_RECOGNITION_SERVICE is null]")
            null
        }else ComponentName.unflattenFromString(serviceComponent)
        debug("ComponentName = $componentName")
*/


        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer.setRecognitionListener(recognitionListener)
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startListen()
        }else{
            debug("Not starting recognizer as Permission RECORD_AUDIO is not granted yet.")
        }
    }

    private fun startListen() {
        val iN = RecognizerIntent.getVoiceDetailsIntent(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.let { i ->
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "US-en")
            i.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            recognizer.startListening(i)
        }
    }

    private fun showSetupNotification(nm: NotificationManager) {
        val pendingIntent =
            PendingIntent.getActivity(this, 9090, Intent(this, SettingsActivity::class.java), 0)

        val n = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(this, Notifications.SETUP_CHANNEL_ID)
        } else {
            NotificationCompat.Builder(this)
        }
        n.setStyle(
            NotificationCompat.InboxStyle()
                .setBigContentTitle("Wave Assistant setup is incomplete")
                .addLine("Click here to continue to setup Wave Assistant")
                .addLine("We assure you that the setup will be as easy and quick as possible")
                .setSummaryText("Setup")
        )
        n.setSmallIcon(R.drawable.ic_logo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            n.setColorized(true)
        }
        n.color = Color.RED
        n.setAutoCancel(true)
        n.setContentIntent(pendingIntent)
        nm.notify("tag.setup", Random.nextInt(), n.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannels(nm: NotificationManager) {
        nm.createNotificationChannel(Notifications.getSetupChannel())
        nm.createNotificationChannel(Notifications.getOthersChannel())
    }


    private val recognitionListener: RecognitionListener = object : RecognitionListener {

        override fun onBeginningOfSpeech() {
            debug("onBeginningOfSpeech")
        }

        override fun onEndOfSpeech() {
            debug("onEndOfSpeech")
        }

        override fun onError(error: Int) {
            debug("onError(error: Int) =$error")
            startListen()
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            debug("onEvent(eventType: Int, params: Bundle?) =Event{${eventType}} && params=$params")
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            debug("onBufferReceived(buffer: ByteArray?) =$buffer")

        }

        override fun onPartialResults(partialResults: Bundle?) {
            debug("onPartialResults(partialResults: Bundle?) =$partialResults")
            if (partialResults != null) {
                val query =
                    partialResults!!.getStringArrayList(RecognizerIntent.EXTRA_PARTIAL_RESULTS)
                        ?.first()
                        ?.toLowerCase(
                            Locale.ROOT
                        )?.trim()
                debug("query = $query")
                if (query?.contains("hey wave", true) == true || query?.contains(
                        "okay wave",
                        true
                    ) == true
                ) {
                    startActivity(Intent(this@MainInteractionService, MainActivity::class.java))
                    startListen()
                }
            }
        }

        override fun onReadyForSpeech(params: Bundle?) {
            debug("onReadyForSpeech(params: Bundle?) =$params")
        }

        override fun onRmsChanged(rmsdB: Float) {
            debug("onRmsChanged(rmsdB: Float) =$rmsdB")
        }

        override fun onResults(results: Bundle?) {
            debug("onResults(results: Bundle?) =$results")
            if (results != null) {
                val query =
                    results!!.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)!!.first()
                        .toLowerCase(
                            Locale.ROOT
                        ).trim()
                debug("query = $query")
                if (query.contains("hey wave", true) || query.contains("okay wave", true)) {
                    startActivity(Intent(this@MainInteractionService, MainActivity::class.java))
                    startListen()
                }
            }

        }

    }

    override fun onGetSupportedVoiceActions(voiceActions: MutableSet<String>): MutableSet<String> {
        val result = (voiceActions)
        debug("onGetSupportedVoiceActions: VoiceActions=$result")
        return result
    }

}

