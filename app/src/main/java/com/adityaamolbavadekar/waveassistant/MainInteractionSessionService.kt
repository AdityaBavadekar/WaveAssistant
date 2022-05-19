package com.adityaamolbavadekar.waveassistant

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

class MainInteractionSessionService : VoiceInteractionSessionService() {

    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        debug("onNewSession()")
        return MainInteractionSession(this)
    }



}


