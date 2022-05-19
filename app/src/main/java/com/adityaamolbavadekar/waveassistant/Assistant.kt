package com.adityaamolbavadekar.waveassistant

import android.accounts.Account
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.annotation.RequiresApi
import com.adityaamolbavadekar.waveassistant.PreferenceItemAdapter.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

class Assistant{

    companion object{
        const val USER_NAME = "android.waveassistant.user.name"
        const val USER_NICK_NAME = "android.waveassistant.user.nick_name"
        const val USER_AGE = "android.waveassistant.user.age"
        const val USER_BIRTHDAY = "android.waveassistant.user.birthday"
        const val USER_EMAIL_ADDRESS = "android.waveassistant.user.google_email"
        const val OUTPUT = "outputString"
        const val ANSWERED = "answeredBoolean"
        const val SETUP_COMPLETED = "waveassistantSetupCompleted"
        const val SETUP_COMPLETED_TRUE = 9090
        const val SETUP_COMPLETED_FALSE = 404
        const val DEFAULT_QUEUE_MODE = TextToSpeech.QUEUE_FLUSH

        const val SMS_DEFAULT_APPLICATION = "sms_default_application"
        const val DIALER_DEFAULT_APPLICATION = "dialer_default_application"
        const val CALL_SCREENING_DEFAULT_APPLICATION = "call_screening_default_component"
        const val ASSISTANT_DEFAULT_APPLICATION = "assistant"
        const val GOOGLE_HELP_ACTIVITY = "com.google.android.gms.googlehelp.helpactivities.HelpActivity"
        const val GOOGLE_MANAGE_ACCOUNT_ACTIVITY = "com.google.android.gms.googlehelp.helpactivities.HelpActivity"

        private const val GOOGLE_SEARCH_PREFIX = "https://www.google.com/search?q="
        fun getGoogleSearchUrl(topic:String): String {
            return GOOGLE_SEARCH_PREFIX+topic
        }
    }

    /*Search*/
    class Search {

        var api: GoogleCustomSearchApi? = null

        init {
//            initialise()
        }

        private fun initialise(){
            if (api != null) return
            val b = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            api = b.create<GoogleCustomSearchApi>()
        }

/*
        fun search(q:String){
            CoroutineScope(Dispatchers.IO).launch {
                initialise()
                val response = api!!.search(q)
                Log.d(TAG,"RESPONSE message= ${response.message()}")
                Log.d(TAG,"RESPONSE headers= ${response.headers()}")
                Log.d(TAG,"RESPONSE body= ${response.body()}")
                Log.d(TAG,"RESPONSE errorBody= ${response.errorBody()}")
            }
        }
*/

        companion object{
            private const val TAG = "Assistant.Search"
            private const val API_KEY = "AIzaSyBXsj1-HZMtCv50F566zJDllJuE5fVDX6k"
            private const val SEARCH_ENGINE_ID = "4f8678047c402451e"
            private const val BASE_URL = "https://www.googleapis.com/"
            private const val _URL = "https://www.googleapis.com/customsearch/v1?"
            const val URL = "${BASE_URL}key=${API_KEY}&cx=$SEARCH_ENGINE_ID&q="
        }

    }

    /*Preferences*/
    class Preferences {

        companion object{
            val prefs = listOf(

                PreferenceItem(PreferenceType.Highlighted_Preference,"About You","",Category.You),
                PreferenceItem(PreferenceType.Highlighted_Preference,"About You","",Category.Account),
                PreferenceItem(PreferenceType.Preference,"Language","",Category.Language),
                PreferenceItem(PreferenceType.Preference,"Voice Recognition","",Category.Voice_Recognition),
                PreferenceItem(PreferenceType.Preference,"Appearance","",Category.Appearance),
                PreferenceItem(PreferenceType.Preference,"Basic info","",Category.Basic_Info),
                PreferenceItem(PreferenceType.Highlighted_Preference,"Personal Results","",Category.Personal_Results),
                PreferenceItem(PreferenceType.Preference,"Notifications","",Category.Notifications),
                PreferenceItem(PreferenceType.Preference,"News","",Category.News),
                PreferenceItem(PreferenceType.Preference,"Music","",Category.Music),
                PreferenceItem(PreferenceType.Preference,"Notes","",Category.Notes),
                PreferenceItem(PreferenceType.Highlighted_Preference,"Your data in Assistant","",Category.Your_Data_In_Assistant),

            )

        }

        enum class Category(val title:String, val description:String,val onClick : ()-> Unit){
            You("About You","Your personal info and data",{}),
            Account("Your Account","Information provided by Google when you sign in",{}),
            Personal_Results("Personal Results","Result based on what you set",{}),
            Language("Languages","",{}),
            Voice_Recognition("Voice Recognition","",{}),
            Appearance("Appearance","Customise how your assistant looks",{}),
            Basic_Info("Basic Info","Add/remove your basic information",{}),
            Notifications("Notifications","",{}),
            News("News","",{}),
            Music("Music","Add/remove music streaming services",{}),
            Notes("Notes","Open Wave Notes",{}),
            Your_Data_In_Assistant("Your data in Assistant","Information like what questions you asked",{})
        }

        enum class Keys(val key:String){
            Provide_Voice_Feedback("android.waveassistant.voice.provide_feedback"),
            Voice_Speech_Rate("android.waveassistant.voice.rate"),
            User_Name(USER_NAME),
            User_Nick_Name(USER_NICK_NAME),
            User_Birthday(USER_BIRTHDAY),
            User_Age(USER_AGE),
            User_Phone("android.waveassistant.user.phone"),
            User_Activity("android.waveassistant.user.activity"),
        }

    }

    /*Actions*/
    class Actions{

        companion object{
            const val ACTION_INTENT_DEVICE_SETTINGS = "android.settings.SETTINGS"
            const val ACTION_TURN_TORCH_ON = "android.feature.torch.on"
            const val ACTION_TURN_TORCH_OFF = "android.feature.torch.off"
            const val ACTION_BLUETOOTH_ON = "android.feature.bluetooth.on"
            const val ACTION_BLUETOOTH_OFF = "android.feature.bluetooth.off"
            const val ACTION_WIFI_ON = "android.feature.wifi.on"
            const val ACTION_WIFI_OFF = "android.feature.wifi.off"
            const val ACTION_INTENT_PHONE = "android.intent.action.DIAL"
            const val ACTION_CONTACTS = "android.open.info.contacts"
            const val ACTION_GALLERY = "android.open.info.gallery"
            const val ACTION_CAMERA = "android.open.info.camera"
            const val ACTION_ASSIST_SETTINGS = "android.open.settings.assistant"
            const val ACTION_BIRTHDAY = "android.action.user.birthday"
            const val ACTION_NAME = "android.action.user.name"
            const val ACTION_NONE = "action.none"


            private const val ACTION_OPEN_PREFIX = "android.open.info."
            fun createActionOpen(openable: String): String {
                return ACTION_OPEN_PREFIX+openable.trim()
            }
            fun getActionOpen(actionOpen:String): String {
                return actionOpen.replace(ACTION_OPEN_PREFIX,"").trim()
            }
            fun identifyActionOpen(actionOpen:String): Boolean {
                return actionOpen.startsWith(ACTION_OPEN_PREFIX,true)
            }

            fun identifyActionSendMail(actionMail: String): Boolean {
                return actionMail.startsWith(SendEmailMetadata.MailToString,true)
            }

            fun createActionEmail(sendEmailMetadata:SendEmailMetadata): String {
                return sendEmailMetadata.toString()
            }

            fun getActionEmail(actionMail:String): SendEmailMetadata {
                val metadata = actionMail.split(",")
                val mailTo = metadata.first().replace(SendEmailMetadata.MailToString,"")
                val mailMessage = metadata.last().replace(SendEmailMetadata.MessageString,"")
                return SendEmailMetadata(mailTo, mailMessage)
            }

        }

        data class SendEmailMetadata(val mailTo: String,val mailMessage:String) {

            override fun toString(): String {
                return "MAIL_TO=$mailTo,MAIL_MESSAGE=$mailMessage"
            }

            companion object{
                const val MailToString = "MAIL_TO="
                const val MessageString = "MAIL_MESSAGE="
            }

        }

    }

    /*Notifications*/
    class Notifications{

        companion object{
            const val SETUP_CHANNEL_ID = "waveassistant.channel.setup"
            const val SETUP_CHANNEL_NAME = "Assistant Setup"
            const val OTHERS_CHANNEL_ID = "waveassistant.channel.others"
            const val OTHERS_CHANNEL_NAME = "Others"


            @RequiresApi(Build.VERSION_CODES.O)
            fun getSetupChannel(): NotificationChannel {
                return NotificationChannel(SETUP_CHANNEL_ID, SETUP_CHANNEL_NAME,NotificationManager.IMPORTANCE_HIGH)
            }

            @RequiresApi(Build.VERSION_CODES.O)
            fun getOthersChannel(): NotificationChannel {
                return NotificationChannel(OTHERS_CHANNEL_ID, OTHERS_CHANNEL_NAME,NotificationManager.IMPORTANCE_HIGH)
            }
        }

    }

    /*Permission*/
    class Permissions{

        companion object{
            val PERMISSIONS = arrayOf(
                android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.WRITE_CONTACTS,
                android.Manifest.permission.GET_ACCOUNTS,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_ADMIN,
                android.Manifest.permission.ACCESS_WIFI_STATE,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.SET_ALARM,
                android.Manifest.permission.READ_SMS,
                android.Manifest.permission.SYSTEM_ALERT_WINDOW,
            )
        }

    }

}