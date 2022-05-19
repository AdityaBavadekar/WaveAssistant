package com.adityaamolbavadekar.waveassistant

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Window
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.adityaamolbavadekar.pinlog.extensions.logD
import com.hypertrack.hyperlog.HyperLog
import kotlin.random.Random

fun List<String>.randomChoice(): String {
    return if (size > 1) {
        random()
    } else {
        first()
    }
}


fun Context.debug(message: String) {
    HyperLog.i(this.javaClass.simpleName, message)
    logD(message)
}


fun Context.hideKeyboard(v: Window) {
    val im = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    im.hideSoftInputFromWindow(v.decorView.windowToken, 0)
}


fun List<Chats>.getLabels(): List<String> {
    val labels = mutableListOf<String>()
    forEach { chat ->
        labels.add(chat.label)
    }
    return labels
}

fun List<Chats>.getAnswers(): List<String> {
    val answers = mutableListOf<String>()
    forEach { chat ->
        answers.addAll(chat.answers)
    }
    return answers
}

fun List<Chats>.getQuestions(): List<String> {
    val questions = mutableListOf<String>()
    forEach { chat ->
        questions.addAll(chat.questions)
    }
    return questions
}

fun Context.prefs(): SharedPreferences {
    return PreferenceManager.getDefaultSharedPreferences(this)
}


fun List<ApplicationInfo>.toInstalledApps(context: Context): List<TextQuery.Worker.InstalledApp> {
    val installedApps = mutableListOf<TextQuery.Worker.InstalledApp>()
    forEach { app ->
        context.debug("app = $app")
        val installedApp = TextQuery.Worker.InstalledApp(app.name, app.packageName)
        installedApps.add(installedApp)
    }
    return installedApps
}


fun TextToSpeech?.speakMessage(text: String) {
    if (this != null) {
//        setSpeechRate(80F)
        speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), Random.nextInt().toString())
    }
}


fun String.isYesNoResponse(): Boolean {
    return (this == "yes" || this == "okay" || this == "ok" || this == "no" || this == "cancel" || this == "nope" || this == "no please" || this == "yes please")
}

