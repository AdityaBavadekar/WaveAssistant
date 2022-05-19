package com.adityaamolbavadekar.waveassistant

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import com.adityaamolbavadekar.waveassistant.databinding.DebugActivityBinding

class SetAsDefaultActivity : AppCompatActivity() {

    private lateinit var binding: DebugActivityBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DebugActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.progress.show()
        debug("onCreate")
        viewModel.startLoadingLogs()
        viewModel.getLogs().observe(this) { logs ->
            var text = ""
            logs.forEach {
                text += "\n$it"
            }
            binding.textView.text = text
            binding.progress.isGone = true
        }


    }


}