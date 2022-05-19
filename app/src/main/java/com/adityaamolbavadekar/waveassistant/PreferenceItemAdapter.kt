package com.adityaamolbavadekar.waveassistant

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.adityaamolbavadekar.waveassistant.databinding.PreferenceItemBinding

class PreferenceItemAdapter : RecyclerView.Adapter<PreferenceItemAdapter.ViewHolder>() {

    private var items = Assistant.Preferences.prefs

    class ViewHolder(val binding: PreferenceItemBinding) :
        RecyclerView.ViewHolder(binding.root) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            PreferenceItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.apply {
            when (item.type) {

                PreferenceType.Preference -> {
                    binding.prefLayout.isVisible = true
                    binding.titleTextView.text = item.catageory.title
                    binding.descriptionTextView.text = item.catageory.description
                }

                PreferenceType.Highlighted_Preference -> {
                    binding.highlightedPref.isVisible = true
                    binding.textViewTitle.text = item.catageory.title
                    binding.textViewDesc.text = item.catageory.description
                }

                else -> {
                }
            }
            binding.root.setOnClickListener {
                Toast.makeText(itemView.context, item.title, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    data class PreferenceItem(
        var type: PreferenceType,
        var title: String,
        var description: String,
        var catageory: Assistant.Preferences.Category
    )

    enum class PreferenceType {
        Preference,
        Highlighted_Preference,
        GroupHeader,
        Header,
        Footer
        ;
    }

}
