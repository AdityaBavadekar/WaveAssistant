package com.adityaamolbavadekar.waveassistant

import android.accounts.Account
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adityaamolbavadekar.waveassistant.databinding.BottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.util.*
import kotlin.random.Random

class MainActivity : AppCompatActivity(), TextQuery.Analyser.OnQueryAnalysisCompleteListener,
    TextQuery.Loader.OnLoadCompleteListener, RecyclerView.OnItemTouchListener {

    private lateinit var engine: TextToSpeech
    private val initListener: TextToSpeech.OnInitListener =
        TextToSpeech.OnInitListener { status: Int ->
            if (status == TextToSpeech.SUCCESS) {
                engine.voice = engine.voices.first()
                if (!prefs().getBoolean(Assistant.SETUP_COMPLETED, false)) {
                    engine.speak(
                        "Please complete setup",
                        TextToSpeech.QUEUE_FLUSH,
                        Bundle(),
                        Random.nextInt().toString()
                    )
                }
            } else {
                debug("TextToSpeech.OnInitListener=$status")
            }
        }
    private val bottomSheetCallback: BottomSheetBehavior.BottomSheetCallback = object :
        BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
                BottomSheetBehavior.STATE_HIDDEN -> {
                    finishAndRemoveTask()
                }
                BottomSheetBehavior.STATE_COLLAPSED -> {
                }
                BottomSheetBehavior.STATE_DRAGGING -> {
                }
                BottomSheetBehavior.STATE_EXPANDED -> {
                }
                BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                }
                BottomSheetBehavior.STATE_SETTLING -> {
                }
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {

        }
    }
    private var analyser: TextQuery.Analyser? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private val intentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK && it.data != null) {
                it.data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.let { list ->
                    onQuery(list[0].toString())
                }
                binding.statusTextView.text = getString(R.string.app_name)
                binding.mic.setImageResource(R.drawable.ic_mic)
            }
        }
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            debug("Permission Result=$it")
        }
    private lateinit var toast: Toast
    private lateinit var binding: BottomSheetBinding
    private lateinit var itemAdapter: ItemAdapter

    @SuppressLint("ShowToast", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            window.setElevation(12F)
        }
        super.onCreate(savedInstanceState)
        debug("onCreate")
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = BottomSheetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        bottomSheetBehavior = BottomSheetBehavior.from(binding.persistentBottomSheet)
//        bottomSheetBehavior.maxHeight = binding.rootLayout.height.minus(50)
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)
        binding.persistentBottomSheet.setOnTouchListener { v, event ->
            binding.itemRecyclerView.onTouchEvent(event)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            binding.persistentBottomSheet.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                binding.itemRecyclerView.onScrolled(scrollX, scrollY)
            }
        }
        binding.itemRecyclerView.addOnItemTouchListener(this)
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomInsetView, bottomWindowInset)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, rootWindowInset)
        val h = binding.linearLayout.height
        binding.itemRecyclerView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            this.bottomMargin = h
        }
        TextQuery.Loader().load(this, this)
        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
        setupCLickListeners()
        setupRecyclerView()
        engine = TextToSpeech(this, initListener)
        permissionLauncher.launch(Assistant.Permissions.PERMISSIONS)
    }

    private fun setupRecyclerView() {
        itemAdapter = ItemAdapter()
        binding.itemRecyclerView.apply {
            adapter = itemAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
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
                binding.statusTextView.text = "Now listening..."
                intentLauncher.launch(this)
            }
        }

        binding.keyboard.setOnClickListener {
            binding.queryEditText.apply {
                visibility = if (isVisible) {
                    View.GONE
                } else {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    toast.setText("Please type to ask")
                    toast.show()
                    View.VISIBLE
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
            finishAndRemoveTask()
        }
    }

    private fun onQuery(query: String) {
        if (query.trim().isNotEmpty()) {
            binding.infoTextView.isGone = true
            binding.statusTextView.text = getString(R.string.app_name)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetBehavior.isDraggable = true
            binding.itemRecyclerView.isVisible = true
        }
        itemAdapter.submitItem(
            ItemAdapter.Item(
                ItemAdapter.ItemType.QUESTION,
                query
            )
        )
//        try {
//            binding.itemRecyclerView.smoothScrollToPosition(pos)
//        } catch (e: Exception) {
//        }
        analyser?.analyse(query, this)
    }

    override fun onAnalysisCompleted(queryResult: QueryResult) {
        debug("onAnalysisCompleted=${queryResult}")
        itemAdapter.submitItem(
            ItemAdapter.Item(
                ItemAdapter.ItemType.ANSWER,
                queryResult.answerText
            )
        )
    }

    private val rootWindowInset = OnApplyWindowInsetsListener { v, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = insets.top //+ 60
        }
        WindowInsetsCompat.CONSUMED
    }

    private val bottomWindowInset = OnApplyWindowInsetsListener { v, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
//        binding.bottomInsetView.minimumHeight = insets.bottom
        v.updatePadding(0, 0, 0, insets.bottom)
        debug("Updated Padding")
        v.updateLayoutParams<LinearLayout.LayoutParams> {
            bottomMargin = insets.bottom
        }
        WindowInsetsCompat.CONSUMED
    }

    override fun onLoaded(
        list: List<Chats>?,
        contacts: List<TextQuery.Loader.ContactItem>,
        accounts: List<Account>
    ) {
        analyser = TextQuery.Analyser(this, list)
        analyser?.contacts = contacts
        analyser?.accounts = accounts
    }


    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        Log.d(TAG, "RecyclerView=onInterceptTouchEvent")
        return true
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        Log.d(TAG, "RecyclerView=onTouchEvent")
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        Log.d(TAG, "RecyclerView=onRequestDisallowInterceptTouchEvent")
    }

    override fun finishAndRemoveTask() {
        debug("finishAndRemoveTask")
        super.finishAndRemoveTask()
    }

    companion object {
        const val TAG = "MainActivity"
    }


}