package com.adityaamolbavadekar.waveassistant

import android.accounts.Account
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adityaamolbavadekar.waveassistant.databinding.ActivitySimpleMainBinding
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class SimpleMainActivity : AppCompatActivity(), TextQuery.Analyser.OnQueryAnalysisCompleteListener,
    TextQuery.Loader.OnLoadCompleteListener {

    private var engine: TextToSpeech? = null
    private var engineSuccess = false
    private val viewModel: MainViewModel by viewModels()
    private val initListener: TextToSpeech.OnInitListener =
        TextToSpeech.OnInitListener { status: Int ->
            if (status == TextToSpeech.SUCCESS) {
                debug("voice initialisation started")
                engineSuccess = true
                worker!!.speaker = engine!!
                try {
                    engine!!.voice = (engine!!.defaultVoice)
                } catch (e: Exception) {
                    debug("Unable to set voice = $e")
                }
                debug("voice initialised")
                if (!prefs().getBoolean(Assistant.SETUP_COMPLETED, false)) {
                    engine.speakMessage(getString(R.string.setup_incomplete))
                }
            } else {
                debug("TextToSpeech.OnInitListener=$status")
            }
        }

    private fun onSetupIncomplete() {
        val b = MaterialAlertDialogBuilder(this, R.style.WaveAssistantDialogTheme)
        b.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            setupIntentLauncher.launch(Intent(this@SimpleMainActivity, SetupActivity::class.java))
        }
        b.setTitle(getString(R.string.setup_incomplete))
        b.setCancelable(false)
        b.setMessage(getString(R.string.setup_incomplete_text))
        b.create()
        b.show()
        engine.speakMessage(getString(R.string.setup_incomplete_assistant_readable_text))
    }

    private var analyser: TextQuery.Analyser? = null
    private var worker: TextQuery.Worker? = null
    private val intentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK && it.data != null) {
                it.data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.let { list ->
                    onQuery(list[0].toString())
                }
                binding.mic.setImageResource(R.drawable.ic_mic)
            }
            binding.progress.hide()

        }
    private val setupIntentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Assistant.SETUP_COMPLETED_TRUE) {
                prefs().edit {
                    putBoolean(Assistant.SETUP_COMPLETED, true)
                }
            } else {
                onSetupIncomplete()
            }
        }

    /**
     * For [TextQuery.Worker.analyseAction] and firing intent with back stack
     * */
    val workerIntentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        }
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            debug("Permission Result=$it")
            //Check if setup is incomplete
            if (!prefs().getBoolean(Assistant.SETUP_COMPLETED, false)) {
                debug("setup incomplete")
                setupIntentLauncher.launch(Intent(this, SetupActivity::class.java))
            }
        }

    private lateinit var toast: Toast
    private lateinit var binding: ActivitySimpleMainBinding
    private lateinit var itemAdapter: ItemAdapter
    private var onCreateCalled = false

    @SuppressLint("ShowToast", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            window.setElevation(12F)
        }
        super.onCreate(savedInstanceState)
        debug("onCreate")
        onCreateCalled = true
        val start = System.currentTimeMillis()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivitySimpleMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomInsetView, bottomWindowInset)
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, rootWindowInset)
        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
        setupCLickListeners()
        setupRecyclerView()
        setupWebView()
        binding.progress.hide()
        worker = TextQuery.Worker(this, itemAdapter, binding.progress)
        viewModel.initWorker(worker!!, contentResolver)
        //Enable TextToSpeech only if user has not disabled it
        if (prefs().getBoolean(Assistant.Preferences.Keys.Provide_Voice_Feedback.key, true)) {
            engine = TextToSpeech(this, initListener)
        }
        permissionLauncher.launch(Assistant.Permissions.PERMISSIONS)
        viewModel.load(this, context = this)

        val end = System.currentTimeMillis()
        debug("Start Time = START=$start][END=$end][time=${end - start}ms]")
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard(window)
    }

    override fun onDestroy() {
        super.onDestroy()
        worker?.speaker = null
        engine?.shutdown()
        hideKeyboard(window)
    }

    private fun setupWebView() {
        binding.webView.apply {
            settings.javaScriptEnabled = true
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                settings.safeBrowsingEnabled = true
            }
            settings.setSupportZoom(false)
            webViewClient = ApplicationWebClient(this@SimpleMainActivity)
        }
    }

    inner class ApplicationWebClient(private val context: Context) : WebViewClient() {

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            binding.progress.show()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            binding.progress.hide()
        }


        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)

            val b = AlertDialog.Builder(context)
            b.setTitle("Loading Problem")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                b.setMessage("Problem occurred while loading ${request!!.url}\n\nSuspected cause : " + error!!.description)
            } else b.setMessage("Problem occurred while loading ${request!!.url}")
            b.create()
            b.show()

            binding.webView.isGone = true
            binding.itemRecyclerView.isVisible = true
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            var ret = false
            request?.let { r ->
                ret =
                    if (r.url.host == "www.google.com" || r.url.host == "www.en.m.wikipedia.org") {
                        false
                    } else {
                        binding.webView.isGone = true
                        binding.itemRecyclerView.isVisible = true
                        true
                    }
            }
            return ret
        }

    }

    private fun setupRecyclerView() {
        itemAdapter = ItemAdapter()
        binding.itemRecyclerView.apply {
            adapter = itemAdapter
            layoutManager = LinearLayoutManager(this@SimpleMainActivity)
        }
    }

    private fun setupCLickListeners() {

        binding.mic.setOnClickListener {
            binding.mic.setImageResource(R.drawable.ic_round_hearing_24)
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Start Speaking")
                binding.progress.show()
                try {
                    intentLauncher.launch(this)
                } catch (e: Exception) {
                    debug("Error Launching ACTION_RECOGNIZE_SPEECH Intent = $e")
                    toast.setText("Please make sure you have Latest version of Google App and it is not disabled")
                    toast.duration = Toast.LENGTH_LONG
                    toast.show()
                    toast.duration = Toast.LENGTH_SHORT
                }
            }
        }

        binding.queryEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val query = binding.queryEditText.text.toString()
                binding.queryEditText.text = null
                if (query.trim().isNotEmpty()) {
                    onQuery(query)
                }
            }
            true
        }

        binding.rootLayout.setOnClickListener {
//            finishAndRemoveTask()
        }
    }

    private fun onQuery(query: String) {
        if (query.trim().isNotEmpty()) {
            binding.infoTextView.isGone = true
            binding.itemRecyclerView.isVisible = true
        }
        itemAdapter.submitItem(
            ItemAdapter.Item(
                ItemAdapter.ItemType.QUESTION,
                query
            )
        )
        viewModel.analyse(query, this)
    }

    private val rootWindowInset = OnApplyWindowInsetsListener { v, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = insets.top + 60
        }
        WindowInsetsCompat.CONSUMED
    }

    private val bottomWindowInset = OnApplyWindowInsetsListener { v, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.updatePadding(0, 0, 0, insets.bottom)
        debug("Updated Padding")
        v.updateLayoutParams<LinearLayout.LayoutParams> {
            bottomMargin = insets.bottom
        }
        WindowInsetsCompat.CONSUMED
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        if (keyCode == KeyEvent.KEYCODE_BACK && binding.webView.isVisible && binding.webView.canGoBack()) {
            binding.webView.goBack()
            return true
        }
        binding.webView.clearHistory()
        return super.onKeyDown(keyCode, event)
    }

    override fun onLoaded(
        list: List<Chats>?,
        contacts: List<TextQuery.Loader.ContactItem>,
        accounts: List<Account>
    ) {
        debug("onLoaded")
        analyser = TextQuery.Analyser(this, list)
        analyser!!.contacts = contacts
        analyser!!.accounts = accounts
        viewModel.initAnalyser(analyser)
    }

    override fun onAnalysisCompleted(queryResult: QueryResult) {
        debug("onAnalysisCompleted=${queryResult}")
        if (queryResult.queryLabelType == TextQuery.LabelType.SEARCH) {
            viewModel.insertIntoQueryActivity(queryResult, contentResolver)
            CoroutineScope(Dispatchers.Main).launch {
                itemAdapter.submitItem(
                    ItemAdapter.Item(
                        ItemAdapter.ItemType.ANSWER,
                        queryResult.answerText
                    )
                )
                engine.speakMessage(queryResult.answerText)
                val url = Assistant.getGoogleSearchUrl(queryResult.action.trimStart())
                binding.webView.loadUrl(url)
                binding.itemRecyclerView.isGone = true
                binding.webView.isVisible = true
            }
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                binding.webView.isGone = true
                binding.itemRecyclerView.isVisible = true
            }
            viewModel.workOn(queryResult)
        }
    }

    override fun onResume() {
        super.onResume()
        if (onCreateCalled) initEngine()
    }

    private fun initEngine() {
        engine?.let {
            val provideVoiceFeedback =
                prefs().getBoolean(Assistant.Preferences.Keys.Provide_Voice_Feedback.key, true)
            if (provideVoiceFeedback && !engineSuccess) {
                engine = TextToSpeech(this, initListener)
            } else if (!provideVoiceFeedback && engineSuccess) {
                worker?.speaker = null
                engine!!.shutdown()
            }
        }
    }

}