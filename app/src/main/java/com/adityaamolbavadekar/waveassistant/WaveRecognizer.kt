package com.adityaamolbavadekar.waveassistant

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.SpeechRecognizer
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.*
import kotlin.system.exitProcess

class WaveRecognizer(@NonNull private val context: Context) {

    private var listenablePhrases = mutableListOf<String>()
    private var recorder: AudioRecord
    private var recognitionThread: RecognitionThread? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false

    init {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            log("Manifest.permission.RECORD_AUDIO is not granted!")
            throw IllegalStateException("Manifest.permission.RECORD_AUDIO is not granted! for ${context.javaClass.name}")
        }
        recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            UNIVERSAL_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            BUFFER_SIZE
        )
        if (recorder.state == AudioRecord.STATE_UNINITIALIZED) {
            recorder.release()
            throw IOException("Microphone might be already in use")
        } else if (recorder.state == AudioRecord.STATE_INITIALIZED) {
            log("WaveRecognizer Initialized successfully")
        }
    }

    private var listeners: MutableList<RecognitionStateListener> = mutableListOf()

    /**
     * Adds listener
     * */
    fun addStateListener(recognitionStateListener: RecognitionStateListener): Boolean {
        var wasAdded: Boolean
        synchronized(listeners) {
            wasAdded = if (listeners.size <= 5) {
                listeners.add(recognitionStateListener)
            } else {
                log("Warning!! Listener was not added as the max limit(5) for listeners has reached. Consider removing a listener to add new listener")
                false
            }
        }
        return wasAdded
    }

    /**
     * Removes listener
     * */
    fun removeStateListener(recognitionStateListener: RecognitionStateListener): Boolean {
        var wasRemoved: Boolean
        synchronized(listeners) {
            wasRemoved = listeners.remove(recognitionStateListener)
        }
        return wasRemoved
    }

    /**
     * Adds listener phrase
     * */
    fun addPhrase(phrase: String): Boolean {
        var wasAdded: Boolean
        synchronized(listenablePhrases) {
            wasAdded = if (listenablePhrases.size <= 5) {
                listenablePhrases.add(phrase)
            } else {
                log("Warning!! Phrase ${phrase} was not added as the max limit(5) for phrases has reached. Consider removing an existing phrase to add new phrases")
                false
            }
        }
        return wasAdded
    }

    /**
     * Removes listener phrase
     * */
    fun removePhrase(phrase: String): Boolean {
        var wasRemoved: Boolean
        synchronized(listenablePhrases) {
            wasRemoved = listenablePhrases.remove(phrase)
        }
        return wasRemoved
    }

    /**
     * Returns list of phrases that you previously added or empty of none exist
     * */
    fun getAddedPhrases(): List<String> {
        return listenablePhrases
    }


    fun startListening() {
        if (isListening) {
            log("Warning! $TAG recognition listener was already listening")
            return
        }

        log("Starting $TAG recognition listener [${Date()}]")
        recognitionThread = RecognitionThread()

    }

    fun stop() {
        if (!isListening) {
            log("Warning! $TAG recognition listener's state is not listening so unable to stop the recognizer")
            recognitionThread?.exit()
            recognitionThread = null
            return
        }

        log("Stopping $TAG recognition listener [${Date()}]")
        recorder.stop()
        recognitionThread?.exit()
        recognitionThread = null
    }

    fun shutdown() {
        if (!isListening) {
            log("Warning! $TAG recognition listener's state is not listening so unable to stop the recognizer")
            recognitionThread?.exit()
            recognitionThread = null
            return
        }

        log("Stopping $TAG recognition listener [${Date()}]")
        recorder.stop()
        recognitionThread?.exit()
        recognitionThread = null
        log("Shutdown of $TAG recognition listener completed [${Date()}]")
    }


    interface RecognitionStateListener {

        /**
         * Called when the endpointer is ready for the user to start speaking.
         *
         * @param params parameters set by the recognition service. Reserved for future use.
         */
        fun onReadyForSpeech(params: Bundle?)

        /**
         * The user has started to speak.
         */
        fun onBeginningOfSpeech()

        /**
         * The sound level in the audio stream has changed. There is no guarantee that this method will
         * be called.
         *
         * @param rmsdB the new RMS dB value
         */
        fun onRmsChanged(rmsdB: Float)

        /**
         * More sound has been received. The purpose of this function is to allow giving feedback to the
         * user regarding the captured audio. There is no guarantee that this method will be called.
         *
         * @param buffer a buffer containing a sequence of big-endian 16-bit integers representing a
         * single channel audio stream. The sample rate is implementation dependent.
         */
        fun onBufferReceived(buffer: ByteArray?)

        /**
         * Called after the user stops speaking.
         */
        fun onEndOfSpeech()

        /**
         * A network or recognition error occurred.
         *
         * @param error code is defined in [SpeechRecognizer]. Implementations need to handle any
         * integer error constant to be passed here beyond constants prefixed with ERROR_.
         */
        fun onError(error: Exception)

        /**
         * Called when recognition results are ready.
         *
         * @param results the recognition results. To retrieve the results in `ArrayList<String>` format use [Bundle.getStringArrayList] with
         * [SpeechRecognizer.RESULTS_RECOGNITION] as a parameter. A float array of
         * confidence values might also be given in [SpeechRecognizer.CONFIDENCE_SCORES].
         */
        fun onResults(results: Bundle?)

        /**
         * Called when partial recognition results are available. The callback might be called at any
         * time between [.onBeginningOfSpeech] and [.onResults] when partial
         * results are ready. This method may be called zero, one or multiple times for each call to
         * [SpeechRecognizer.startListening], depending on the speech recognition
         * service implementation.  To request partial results, use
         * [RecognizerIntent.EXTRA_PARTIAL_RESULTS]
         *
         * @param partialResults the returned results. To retrieve the results in
         * ArrayList&lt;String&gt; format use [Bundle.getStringArrayList] with
         * [SpeechRecognizer.RESULTS_RECOGNITION] as a parameter
         */
        fun onPartialResults(partialResults: Bundle?)

        /**
         * Reserved for adding future events.
         *
         * @param eventType the type of the occurred event
         * @param params a Bundle containing the passed parameters
         */
        fun onEvent(eventType: Int, params: Bundle?)
    }

    companion object {
        const val VOICE_RECOGNITION_SERVICE = "voice_recognition_service"
        const val TAG = "WaveRecognizer"
        const val NO_TIME_OUT = -1
        const val UNIVERSAL_SAMPLE_RATE = 44100
        const val BUFFER_SIZE = DEFAULT_BUFFER_SIZE * 2
    }

    private fun log(message: String) {
        context.debug(message)
    }


    private inner class RecognitionThread(
        private val timeoutInt: Int,
        threadName: String = "RecognitionThread"
    ) : Thread(threadName) {
        private var timeout = NO_TIME_OUT

        constructor() : this(NO_TIME_OUT) {

        }

        init {
            if (timeoutInt != NO_TIME_OUT) {
                timeout = timeoutInt * UNIVERSAL_SAMPLE_RATE / 1000
            }
        }


        override fun run() {
            super.run()

            recorder.startRecording()
            log("Started recording")

            if (recorder.recordingState == AudioRecord.RECORDSTATE_STOPPED) {
                log("Stopping recording. Microphone might be already in use")
                recorder.stop()
                val exception =
                    IOException("Stopping recording. Microphone might be already in use")
                mainHandler.post(OnErrorEvent(listeners, exception))
                return
            }

            val buffer: ShortArray = shortArrayOf(BUFFER_SIZE.toShort())
            var isInSpeech = false

            //Skip
            recorder.read(buffer, 0, buffer.size)

            while (!interrupted() &&
                ((timeout == NO_TIME_OUT) || (timeout > 0))
            ) {
                val inSpeech = true
                val nread = recorder.read(buffer, 0, buffer.size)

                if (-1 == nread) {
                    throw RuntimeException("Error reading audio buffer")
                } else if (nread > 0) {

                }


                if (isInSpeech != inSpeech) {
                    isInSpeech = inSpeech
                }
                mainHandler.post(OnSpeechEvent(listeners, inSpeech))


            }

        }

        fun exit() {
            exitProcess(0)
        }
    }

    private abstract class AudioRecognitionEvent : Runnable {
        private lateinit var listeners: MutableList<RecognitionStateListener>

        constructor(listenersList: MutableList<RecognitionStateListener>) : this() {
            listeners = listenersList
        }

        constructor()

        override fun run() {
            for (recognitionStateListener in listeners) {
                execute(recognitionStateListener)
            }
        }

        abstract fun execute(recognitionStateListener: RecognitionStateListener)
    }

    private class OnSpeechEvent(
        listenersList: MutableList<RecognitionStateListener>,
        private val isInSpeech: Boolean
    ) : AudioRecognitionEvent(listenersList) {
        override fun execute(recognitionStateListener: RecognitionStateListener) {
            if (isInSpeech) recognitionStateListener.onBeginningOfSpeech()
            else recognitionStateListener.onEndOfSpeech()
        }
    }

    private class OnErrorEvent(
        listenersList: MutableList<RecognitionStateListener>,
        private val exception: Exception
    ) : AudioRecognitionEvent(listenersList) {
        override fun execute(recognitionStateListener: RecognitionStateListener) {
            recognitionStateListener.onError(exception)
        }
    }

}
