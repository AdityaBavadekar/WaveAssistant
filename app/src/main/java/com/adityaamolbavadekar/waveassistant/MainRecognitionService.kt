package com.adityaamolbavadekar.waveassistant

import android.content.Intent
import android.speech.RecognitionService
import android.speech.SpeechRecognizer

class MainRecognitionService : RecognitionService() {

    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        debug("onStartListening")
        listener?.beginningOfSpeech()
    }

    override fun onCancel(listener: Callback?) {
        debug("onCancel")
        listener?.error(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
    }


    override fun onStopListening(listener: Callback?) {
        debug("onStopListening")
        listener?.endOfSpeech()
    }

    override fun onDestroy() {
        super.onDestroy()
        debug("onDestroy")
    }

}