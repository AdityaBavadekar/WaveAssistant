package com.adityaamolbavadekar.waveassistant

import android.app.Activity
import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.speech.RecognizerIntent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import com.adityaamolbavadekar.waveassistant.databinding.ActivityMainBinding
import com.adityaamolbavadekar.waveassistant.databinding.BottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior

class MainInteractionSession(private val context: Context) : VoiceInteractionSession(context) {

    override fun getContext(): Context {
        return this.context
    }

    private lateinit var binding: BottomSheetBinding
    private var startIntent : Intent? = null

    override fun onHandleAssist(
        data: Bundle?,
        structure: AssistStructure?,
        content: AssistContent?
    ) {
        super.onHandleAssist(data, structure, content)
    }

    override fun onHandleAssist(state: AssistState) {
        super.onHandleAssist(state)
        context?.debug("${state.assistData?.keySet()}")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            getInfo(state)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getInfo(state: AssistState) {
        context.debug("onHandleAssist")
        state.assistContent?.let {
            if(it.isAppProvidedIntent){
                val keys = it.extras.keySet()
                keys.forEach{key->
                    val value = it.extras.get(key)
                    context.debug("$key = [${value}]")
                }
            }
        }
    }

    override fun onHandleAssistSecondary(
        data: Bundle?,
        structure: AssistStructure?,
        content: AssistContent?,
        index: Int,
        count: Int
    ) {
        super.onHandleAssistSecondary(data, structure, content, index, count)
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        super.onHandleScreenshot(screenshot)

    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        context.debug("onShow")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                closeSystemDialogs()
            }
        } catch (e: Exception) {
        }

        startIntent = Intent(context,SimpleMainActivity::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startAssistantActivity(startIntent)
        }else binding.rootLayout.visibility = View.VISIBLE
    }


    override fun onHide() {
        super.onHide()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            binding.rootLayout.visibility = View.VISIBLE
        }
    }

    override fun onPrepareShow(args: Bundle?, showFlags: Int) {
        super.onPrepareShow(args, showFlags)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setUiEnabled(false)
        }
    }

    override fun onCreateContentView(): View {
        context.debug("onCreateContentView")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return View(context)
        }else{
            val li = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            binding = BottomSheetBinding.inflate(li)
            return binding.rootLayout
        }

    }

    override fun onLockscreenShown() {
        super.onLockscreenShown()
    }


}