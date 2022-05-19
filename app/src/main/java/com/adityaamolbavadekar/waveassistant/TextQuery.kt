package com.adityaamolbavadekar.waveassistant

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.adityaamolbavadekar.waveassistant.Assistant.Actions.Companion.ACTION_NONE
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TextQuery {

    class Analyser(private val context: Context, private val loadResult: List<Chats>?) {

        var contacts: List<Loader.ContactItem>? = null
        var accounts: List<Account>? = null

        private data class PreviousQueryData(
            var rootQuery: String,
            var answer: String,
            var responseWasQuestion: Boolean,
            var responseType: Response
        ) {
            constructor() : this(
                rootQuery = "",
                answer = "",
                responseWasQuestion = false,
                responseType = Response.ANSWERED
            )
        }

        enum class Response() {
            YES_NO,
            completedYES_NO,
            SEND_SMS,
            completedSEND_SMS,
            SEND_EMAIL,
            completedSEND_EMAIL,
            SEND_MESSAGE,
            completedSEND_MESSAGE,
            ANSWERED
        }

        private var queryString: String = ""
        private var rootQueryString: String = ""
        private var queryAction: String = ACTION_NONE
        private var queryAnswersList: List<String> = listOf()
        private var queryLabelType: LabelType = LabelType.UNKNOWN
        private var pd: PreviousQueryData? = null
        private lateinit var onQueryAnalysisCompleteListener: OnQueryAnalysisCompleteListener

        fun analyse(queryText: String, listener: OnQueryAnalysisCompleteListener) {
            this.queryString = removeIgnorables(queryText).toLowerCase(Locale.ROOT).trim()
            this.rootQueryString = queryText
            this.queryLabelType = LabelType.UNKNOWN
            this.queryAction = ACTION_NONE
            this.queryAnswersList = listOf()
            this.onQueryAnalysisCompleteListener = listener
            if (loadResult == null) {
                val queryResult = QueryResult.Builder()
                    .withAction(queryAction)
                    .withPreferredAnswer(errorMessages.randomChoice())
                    .withAnswersList(queryAnswersList)
                    .withLabelType(LabelType.NOT_UNDERSTOOD)
                    .withPhoto(null)
                    .withRootQuery(rootQueryString)
                    .build()

                listener.onAnalysisCompleted(queryResult)
            } else run()
        }

        companion object {

            val errorMessages =
                listOf(
                    "Sorry I couldn\'t understand",
                    "Sorry I didn\'t understand",
                    "Sorry I didn\'t catch that",
                    "Sorry I don\'t know that",
                    "Can you pardon?"
                )
        }

        private fun run() {
            checkPreviousDataMatch()
            val map = checkUserProtocols()

            val output =
                if (map[Assistant.ANSWERED] == "true") {
                    map[Assistant.OUTPUT]!!
                } else {
                    val out = checkOperationProtocols()
                    if (out != "") {
                        out
                    } else {
                        val chat = matchQuestions()
                        if (chat != null) {
                            queryLabelType = LabelType.UNKNOWN
                            queryAnswersList = chat.answers
                            chat.answers.randomChoice()
                        } else {
                            queryLabelType = LabelType.NOT_UNDERSTOOD
                            errorMessages.randomChoice()
                        }
                    }
                }
            val queryResult = QueryResult.Builder()
                .withAction(queryAction)
                .withPreferredAnswer(output)
                .withAnswersList(queryAnswersList)
                .withLabelType(queryLabelType)
                .withRootQuery(rootQueryString)
                .withPhoto(null)
                .build()

            onQueryAnalysisCompleteListener.onAnalysisCompleted(queryResult)
        }

        private fun checkPreviousDataMatch() {

        }

        private fun matchQuestions(): Chats? {
            var match: Chats? = null
            val chatsList = loadResult!!
            chatsList.forEach { chat ->
                chat.questions.forEach { q ->
                    if (removeIgnorables(q.toLowerCase(Locale.ROOT).trim()) ==
                        queryString
                    ) {
                        match = chat
                    }
                }
            }
            return match
        }

        private fun removeIgnorables(s: String): String {
            val pattern = Regex(pattern = """([!,?,.,+,-,@,#,!,$,%,^,&,*,])""")
            return pattern.replace(s, "")
        }

        private fun checkUserProtocols(): HashMap<String, String> {
            var output = ""
            var answered = false

            if (rootQueryString.toLowerCase(Locale.ROOT)
                    .trim() == ("i am?") || rootQueryString.toLowerCase(Locale.ROOT)
                    .trim() == ("my name is?") || queryString
                    .trim() == ("what is my name")
            ) {
                val name = context.prefs().getString(Assistant.USER_NAME, null)

                output =
                    if (name != null) "You told that your name was ${name}." else "You haven\'t told me your name yet.\n\nBut You could tell me today and i\'ll remember it"
                answered = true
            } else {

                if (queryString.contains("i am")) {
                    val name = rootQueryString.replace("i am", "", true)
                    context.prefs().edit {
                        putString(Assistant.USER_NAME, name)
                    }
                    queryAction = Assistant.Actions.ACTION_NAME
                    output = "Okay i\'ll remember your name from now on."
                    answered = true
                }

                if (queryString.contains("my name is")) {
                    val name = rootQueryString.replace("my name is", "", true)
                    context.prefs().edit {
                        putString(Assistant.USER_NAME, name)
                    }
                    queryAction = Assistant.Actions.ACTION_NAME
                    output = "Okay i\'ll remember your name from now on."
                    answered = true
                }

            }

            if (queryString == "today is my birthday" || queryString == "my birthday is today") {
                val prefr = context.prefs()
                prefr.edit {
                    putLong(Assistant.USER_BIRTHDAY, System.currentTimeMillis())
                }
                val name = prefr.getString(Assistant.USER_NAME, null) ?: ""
                queryLabelType = LabelType.BIRTHDAY
                queryAction = Assistant.Actions.ACTION_BIRTHDAY
                output = "Happy Birthday ${name}!\nI\'ll remember your birthday date from now on."
                answered = true
            }

            if (queryString == "when is my birthday") {
                val date: Long = context.prefs().getLong(Assistant.USER_BIRTHDAY, 0.toLong())
                output = if (date == 0.toLong()) {
                    context.getString(R.string.i_don_know_your_birthday_date_yet_but_text)
                } else {
                    val stringDate = SimpleDateFormat("dd MMM", Locale.ENGLISH).format(Date(date))
                    context.getString(R.string.you_told_your_birthday_is_on_text, stringDate)
                }
                answered = true
            }

            return hashMapOf(Assistant.OUTPUT to output, Assistant.ANSWERED to "$answered")
        }

        private fun checkOperationProtocols(): String {
            var output = ""
            if (queryString.contains("what is date today", true) ||
                queryString.contains("what is the date today", true)
                || queryString.contains("whats date today", true)
                || queryString.contains("tell me today's date", true)
                || queryString.contains("tell me the date", true)

            ) {
                queryLabelType = LabelType.DATE_AND_TIME
                output = "It is " + SimpleDateFormat("EEE dd MMM yyyy") +
                        "\n\n" +
                        "and it is " + SimpleDateFormat("hh:mm a")
            }

            if (queryString.startsWith("search", true)) {
                queryLabelType = LabelType.SEARCH
                queryAction = rootQueryString.replace("search", "", true)
                output = "Searching for $queryAction"
            }

            if (queryString.contains("send a mail", true)) {
                queryAction = ACTION_NONE
                queryLabelType = LabelType.SEND_EMAIL
                output = "Okay\n\nWho do you want to send an email?"
                pd = PreviousQueryData(rootQueryString, output, true, Response.SEND_EMAIL)
            }

            if (queryString.contains("send a message", true)) {
                queryAction = ACTION_NONE
                queryLabelType = LabelType.SEND_MESSAGE
                output = "Okay\n\nWho do you want to send a message?"
                pd = PreviousQueryData(rootQueryString, output, true, Response.SEND_MESSAGE)
            }

            if (queryString.contains("send sms", true)) {
                queryAction = ACTION_NONE
                queryLabelType = LabelType.SEND_SMS
                output = "Okay\n\nWho do you want to send a sms?"
                pd = PreviousQueryData(rootQueryString, output, true, Response.SEND_SMS)
            }

            if (queryString.startsWith("open", true)) {
                queryLabelType = LabelType.OPEN_APPLICATION
                if (queryString.replace("open", "", true).trim() == "settings") {
                    output = context.getString(R.string.opening_device_settings)
                    queryAction = Assistant.Actions.ACTION_INTENT_DEVICE_SETTINGS
                } else if (queryString.replace("open", "", true)
                        .trim() == "assistant settings" || queryString.replace("open", "", true)
                        .trim() == "assistant"
                ) {
                    output = context.getString(R.string.opening_assistant)
                    queryAction = Assistant.Actions.ACTION_ASSIST_SETTINGS
                } else {
                    val openable = queryString.replace("open", "", true)
                    queryAction = Assistant.Actions.createActionOpen(openable)
                    output = context.getString(R.string.opening_app_formatted, openable)
                }
            }
            if (queryString.startsWith("turn flashlight", true)) {
                val mode = queryString.replace("turn flashlight", "", true).trim()
                output = when (mode) {//TODO
                    context.getString(R.string.on_text) -> {
                        queryAction = Assistant.Actions.ACTION_TURN_TORCH_ON
                        queryLabelType = LabelType.TOGGLE_TORCH_ON
                        toggleTorch(true)
                    }
                    context.getString(R.string.off_text) -> {
                        queryAction = Assistant.Actions.ACTION_TURN_TORCH_OFF
                        queryLabelType = LabelType.TOGGLE_TORCH_OFF
                        toggleTorch(false)
                    }
                    else -> ""
                }
            }
            if (queryString.startsWith("turn bluetooth", true)) {
                val mode = queryString.replace("turn bluetooth", "", true).trim()
                output = when (mode) {//TODO
                    context.getString(R.string.on_text) -> {
                        queryAction = Assistant.Actions.ACTION_BLUETOOTH_ON
                        queryLabelType = LabelType.TOGGLE_BLUETOOTH_ON
                        context.getString(R.string.turning_bluetooth_on)
                    }
                    context.getString(R.string.off_text) -> {
                        queryAction = Assistant.Actions.ACTION_BLUETOOTH_OFF
                        queryLabelType = LabelType.TOGGLE_BLUETOOTH_OFF
                        context.getString(R.string.turning_bluetooth_off)
                    }
                    else -> ""
                }
            }
            if (queryString.startsWith("turn wifi", true)) {
                val mode = queryString.replace("turn bluetooth", "", true).trim()
                output = when (mode) {//TODO
                    context.getString(R.string.on_text) -> {
                        queryAction = Assistant.Actions.ACTION_BLUETOOTH_ON
                        queryLabelType = LabelType.TOGGLE_BLUETOOTH_ON
                        "Turning WiFi on"//TODO(CHECK PERMISSIONS)
                    }
                    context.getString(R.string.off_text) -> {
                        queryAction = Assistant.Actions.ACTION_BLUETOOTH_OFF
                        queryLabelType = LabelType.TOGGLE_BLUETOOTH_OFF
                        "Turning WiFi off"
                    }
                    else -> ""
                }
            }
            if (queryString.startsWith("play", true)) {
                queryLabelType = LabelType.PLAY_MUSIC
                output = "Okay.\n\nPlaying " + queryString.replace("play", "", true)
            }
            return output
        }

        private fun toggleTorch(b: Boolean): String {
            var output = context.getString(R.string.your_device_does_not_support_flashlight)
            val hasTorch =
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && hasTorch) {
                output = "Turning Flashlight " + if (b) "on" else "off"
            }
            return output
        }

        interface OnQueryAnalysisCompleteListener {
            fun onAnalysisCompleted(queryResult: QueryResult)
        }

    }

    class Loader {
        private var context: Context? = null
        private var contacts: MutableList<ContactItem> = mutableListOf()
        private var accounts: MutableList<Account> = mutableListOf()

        @SuppressLint("Range")
        fun load(listener: OnLoadCompleteListener, contextValue: Context) {
            this.context = contextValue
            context!!.debug("Loader init")
            context!!.debug("App Info [BuildType] = [${BuildConfig.BUILD_TYPE}]")
            context!!.debug("Device Info [Model] = [${Build.MODEL}]")
            context!!.debug("Device Info [Device] = [${Build.DEVICE}]")
            context!!.debug("DefaultLocale = ${Locale.getDefault().displayLanguage}]")
            getAccounts()
            getContacts()
            getShortcuts()
            getProviders()
            getContentProviderData()//For debugging
            val list = AssistantData().getDataValue()
            listener.onLoaded(list, contacts, accounts)
        }

        private fun getContentProviderData() {
            val c = context!!.contentResolver.query(
                QueryActivityProvider.CONTENT_URI,
                arrayOf(QueryActivityProvider.QUERY, QueryActivityProvider.ANSWER), null, null, null
            )
            context!!.debug("Cursor is null = ${(c == null)}")
            context!!.debug("Cursor size = ${c?.count}")
            c?.let {
                if (it.moveToFirst()) {
                    val q = it.getString(0)
                    val a = it.getString(1)
                    context!!.debug("Question [${q}]\nAnswer [${a}]")
                    it.close()
                } else {
                    while (it.moveToNext()) {
                        val q = it.getString(0)
                        val a = it.getString(1)
                        context!!.debug("Question [${q}]\nAnswer [${a}]")
                    }
                }
                it?.close()
            }
        }

        @SuppressLint("Range")
        private fun getContacts() {
            val granted =
                ContextCompat.checkSelfPermission(
                    context!!,
                    android.Manifest.permission.READ_CONTACTS
                )
            if (granted == PackageManager.PERMISSION_GRANTED) {
                val c = context!!.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(),
                    null,
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                )
                if (c == null) {
                    c?.close()
                } else {
                    context!!.debug("c.columnNames = ${c.columnNames.toList()}")
                    while (c.moveToNext()) {
                        val name =
                            c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                        val phone =
                            c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        val photo =
                            c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI))
                                ?: ""
                        contacts.add(ContactItem(name, phone, photo))
                    }
                }
                c?.close()
            } else {
                context!!.debug("Contacts permission not granted")
            }
            contacts.sortBy { it.name }
        }

        data class ContactItem(val name: String, val phoneNumber: String, val photoUri: String?)

        private fun getAccounts() {
            if (ContextCompat.checkSelfPermission(
                    context!!,
                    android.Manifest.permission.GET_ACCOUNTS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val bm = context!!.getSystemService(Context.ACCOUNT_SERVICE) as AccountManager
                accounts.addAll(bm.accounts.sortedBy { it.type })
                bm.accounts.forEach {
                    context!!.debug("Account = ${it.name} [${it.type}]")
                    if (it.type == "com.google") {
                        context!!.prefs().edit {
                            putString(Assistant.USER_EMAIL_ADDRESS, it.name)
                        }
                    }
                }
            } else {
                context!!.debug("Get Accounts permission not granted")
            }
        }

        private fun getShortcuts() {
            val intent = Intent(Intent.ACTION_CREATE_SHORTCUT)
            val shortcuts = context!!.packageManager.queryIntentActivities(intent, 0)
            if (shortcuts != null) {
                shortcuts.forEach {
                    val activity = it.activityInfo.name
                    val appName = it.loadLabel(context!!.packageManager)
                    context!!.debug("Shortcut :[$activity] [$appName]")
                }
            }
        }

        private val BIIntents = listOf(
            Intent("actions.intent.OPEN_APP_FEATURE"),
            Intent("actions.intent.CREATE_THING"),
            Intent("actions.intent.GET_THING"),
        )

        private fun getProviders() {
            val providers = context!!.packageManager.queryContentProviders(null, 0, 0)
            providers?.let {
                it.forEach { provider ->
                    val activity = provider.authority
                    context!!.debug("Authority :[$activity]")
                }
            }

            BIIntents.forEach { i ->
                val shortcuts = context!!.packageManager.queryIntentActivities(i, 0)
                if (shortcuts != null) {
                    shortcuts.forEach {
                        val activity = it.activityInfo.name
                        val appName = it.loadLabel(context!!.packageManager)
                        context!!.debug("BIIntent :[$activity] [$appName]")
                    }
                }

            }
        }

        interface OnLoadCompleteListener {
            fun onLoaded(list: List<Chats>?, contacts: List<ContactItem>, accounts: List<Account>)
        }

    }

    enum class LabelType {
        UNKNOWN,
        NOT_UNDERSTOOD,
        GREETING,
        SEARCH,
        PLAY_MUSIC,
        USER_RELATED_QUESTION,
        ABOUT_WAVE_ASSISTANT,
        MATHEMATICS_QUESTION,
        DEVICE_INFO,
        DEVICE_OPERATION,
        OPEN_APPLICATION,
        DATE_AND_TIME,
        TOGGLE_TORCH_ON,
        TOGGLE_TORCH_OFF,
        TOGGLE_BLUETOOTH_ON,
        TOGGLE_BLUETOOTH_OFF,
        TOGGLE_WIFI_ON,
        TOGGLE_WIFI_OFF,
        CLOSE_WAVE_ASSISTANT,
        BIRTHDAY,
        YES_NO_QUESTION,
        SEND_SMS,
        SEND_EMAIL,
        SEND_MESSAGE
    }

    class Worker(
        private val context: Context,
        private val adapter: ItemAdapter,
        private val progressBar: LinearProgressIndicator
    ) {

        var resolver: ContentResolver? = null
        private lateinit var queryResult: QueryResult
        var speaker: TextToSpeech? = null
        var viewModel: MainViewModel? = null
        private lateinit var installedApps: MutableList<InstalledApp>
        private var action: String = ""

        fun initialise() {
            context.debug("get apps list")
            val appsList = context.packageManager.getInstalledApplications(0)
            installedApps = mutableListOf<InstalledApp>()
            appsList.forEach { app ->
                if (app.packageName != context.packageName) {
                    val installedApp = InstalledApp(
                        context.packageManager.getApplicationLabel(app).toString()
                            .toLowerCase(Locale.ROOT), app.packageName
                    )
                    installedApps.add(installedApp)
                }
            }
            context.debug("apps list was retrieved")
        }

        data class InstalledApp(val appName: String, val appPackageName: String) {
            override fun toString(): String {
                return "\nApp=[$appName][$appPackageName]"
            }
        }

        fun workOn(qr: QueryResult) {
            action = ""
            queryResult = qr
            analyseOpenIntent()
            analyseLabelType()
            analyseAction()
        }

        private fun analyseOpenIntent() {
            if (Assistant.Actions.identifyActionOpen(queryResult.action)) {
                context.debug("identified open")
                val openable = Assistant.Actions.getActionOpen(queryResult.action)
                var openableApplication: InstalledApp? = null
                installedApps.forEach { app ->
                    if (app.appName == openable.trim()) {
                        openableApplication = app
                    }
                }
                if (openableApplication == null) {
                    queryResult.answerText = Analyser.errorMessages.randomChoice()
                } else {
                    queryResult.photo =
                        context.packageManager.getApplicationIcon(openableApplication!!.appPackageName)
                    action = openableApplication!!.appPackageName
                }
            }
        }

        private fun analyseLabelType() {
            val item = ItemAdapter.Item(
                ItemAdapter.ItemType.ANSWER,
                queryResult.answerText,
                queryResult.photo,
                action
            )
            when (queryResult.queryLabelType) {
                LabelType.NOT_UNDERSTOOD -> {
                    item.type = ItemAdapter.ItemType.UNKNOWN_ANSWER
                }
                LabelType.TOGGLE_WIFI_ON -> {
                    item.type = ItemAdapter.ItemType.TOGGLE_WIFI_ON
                }
                LabelType.TOGGLE_WIFI_OFF -> {
                    item.type = ItemAdapter.ItemType.TOGGLE_WIFI_OFF
                }
                LabelType.TOGGLE_BLUETOOTH_ON -> {
                    item.type = ItemAdapter.ItemType.TOGGLE_BLUETOOTH_ON
                }
                LabelType.TOGGLE_BLUETOOTH_OFF -> {
                    item.type = ItemAdapter.ItemType.TOGGLE_BLUETOOTH_OFF
                }
                LabelType.TOGGLE_TORCH_ON -> {
                    item.type = ItemAdapter.ItemType.TOGGLE_TORCH_ON
                }
                LabelType.TOGGLE_TORCH_OFF -> {
                    item.type = ItemAdapter.ItemType.TOGGLE_TORCH_OFF
                }
                LabelType.DATE_AND_TIME -> {
                    item.type = ItemAdapter.ItemType.DATE
                }
                LabelType.BIRTHDAY -> {
                    item.type = ItemAdapter.ItemType.BIRTHDAY
                }
                else -> {
                }
            }
            CoroutineScope(Dispatchers.Main).launch {//RecyclerView issue
                adapter.submitItem(item)
            }
            speaker?.speakMessage(item.text)
            resolver?.let {
                viewModel?.insertIntoQueryActivity(queryResult, it)
            }
        }

        private fun analyseAction() {
            val action = queryResult.action
            when {

                Assistant.Actions.identifyActionOpen(queryResult.action) -> {
                    val openable = Assistant.Actions.getActionOpen(queryResult.action)
                    var openableApplication: InstalledApp? = null
                    installedApps.forEach { app ->
                        if (app.appName == openable.trim()) {
                            openableApplication = app
                        }
                    }
                    openableApplication?.let {
                        context.packageManager.getLaunchIntentForPackage(it.appPackageName)
                            ?.let { intent ->
                                startIntent(intent)
                            }
                    }
                }

                Assistant.Actions.identifyActionSendMail(action) -> {
                    val metadata = Assistant.Actions.getActionEmail(action)
                    val i = Intent(Intent.ACTION_SENDTO)
                    i.putExtra(Intent.EXTRA_TEXT, metadata.mailMessage)
                    i.putExtra(Intent.EXTRA_EMAIL, metadata.mailTo)
                    startIntent(i)
                }

                action == Assistant.Actions.ACTION_TURN_TORCH_ON -> {
                    val cm =
                        context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        cm.setTorchMode(cm.cameraIdList[0], true)
                    }
                }
                action == Assistant.Actions.ACTION_TURN_TORCH_OFF -> {
                    val cm =
                        context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        cm.setTorchMode(cm.cameraIdList[0], false)
                    }
                }
                action == Assistant.Actions.ACTION_INTENT_DEVICE_SETTINGS -> {
                    val i = Intent(Settings.ACTION_SETTINGS)
                    startIntent(i)
                }
                action == Assistant.Actions.ACTION_BLUETOOTH_ON -> {
                }
                action == Assistant.Actions.ACTION_BLUETOOTH_OFF -> {
                }
                action == Assistant.Actions.ACTION_CONTACTS -> {
                    val i = Intent(Intent.ACTION_DEFAULT, ContactsContract.Contacts.CONTENT_URI)
                    startIntent(i)
                }
                action == Assistant.Actions.ACTION_CAMERA -> {
                    val cm =
                        context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    var frontCam = cm.cameraIdList.first()
                    cm.cameraIdList.forEach { camId ->
                        cm.getCameraCharacteristics(camId)[CameraCharacteristics.LENS_FACING]?.let { facing ->
                            if (facing == CameraCharacteristics.LENS_FACING_FRONT) frontCam =
                                camId
                        }
                    }

                    val i = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startIntent(i)
                }
                action == Assistant.Actions.ACTION_GALLERY -> {
                    val i = Intent(Intent.ACTION_VIEW)
                    i.type = "image/*"
                    startIntent(i)
                }
                action == Assistant.Actions.ACTION_INTENT_PHONE -> {
                    val i = Intent(Intent.ACTION_DIAL)
                    startIntent(i)
                }
                action == Assistant.Actions.ACTION_WIFI_ON -> {
                    val wm =
                        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    wm.isWifiEnabled = true
                }
                action == Assistant.Actions.ACTION_WIFI_OFF -> {
                    val wm =
                        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    wm.isWifiEnabled = false
                }
                action == Assistant.Actions.ACTION_ASSIST_SETTINGS -> {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                }

            }
        }

        private fun startIntent(i: Intent) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.debug("Starting User Request OpenIntent=$i")
            try {
                (context as SimpleMainActivity).workerIntentLauncher.launch(i)
            } catch (e: Exception) {
                context.startActivity(i)
            }
        }

    }

}

