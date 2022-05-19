package com.adityaamolbavadekar.waveassistant

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.adityaamolbavadekar.waveassistant.databinding.ActivitySetupBinding

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private lateinit var prefItemAdapter: PreferenceItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        debug("onCreate")
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRecyclerView()
        binding.completeSetup.setOnClickListener {
            setResult(Assistant.SETUP_COMPLETED_TRUE)
            finish()
        }
    }

    private fun setupRecyclerView() {
        prefItemAdapter = PreferenceItemAdapter()
        binding.prefsRecyclerView.apply {
            adapter = prefItemAdapter
            layoutManager = LinearLayoutManager(this@SetupActivity)
        }
    }


}