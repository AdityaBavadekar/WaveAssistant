package com.adityaamolbavadekar.waveassistant

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.adityaamolbavadekar.waveassistant.databinding.RecyclerViewItemBinding

class ItemAdapter : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {


    private fun checkChangedListener(context: Context, type: ItemType) =
        CompoundButton.OnCheckedChangeListener { button, isChecked ->

            when (type) {

                ItemType.TOGGLE_TORCH_ON -> {
                    val cm =
                        context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        cm.setTorchMode(cm.cameraIdList[0], false)
                    }
                }
                ItemType.TOGGLE_TORCH_OFF -> {
                    val cm =
                        context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        cm.setTorchMode(cm.cameraIdList[0], true)
                    }
                }
                ItemType.TOGGLE_WIFI_ON -> {
                    val wm =
                        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    wm.isWifiEnabled = false
                }
                ItemType.TOGGLE_WIFI_OFF -> {
                    val wm =
                        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    wm.isWifiEnabled = true
                }
                else -> {
                }
            }


        }

    private var items = mutableListOf<Item>(Item(ItemType.FIRST_ITEM, "FirstItem"))

    class ViewHolder(val binding: RecyclerViewItemBinding) :
        RecyclerView.ViewHolder(binding.root) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            RecyclerViewItemBinding.inflate(
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
                ItemType.QUESTION -> {
                    binding.questionTextView.text = item.text
                    binding.questionTextView.isVisible = true
                }
                ItemType.ANSWER -> {
                    if (item.drawable == null) {
                        binding.answerTextView.text = item.text
                        binding.answerTextView.isVisible = true
                    } else {
                        binding.appCard.setOnClickListener {
                            val c = itemView.context
                            c.packageManager.getLaunchIntentForPackage(item.action)?.let { intent ->
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                (c as SimpleMainActivity).workerIntentLauncher.launch(intent)
                            }
                        }
                        binding.appAnswerTextView.text = item.text
                        binding.appImageView.setImageDrawable(item.drawable)
                        binding.appCard.isVisible = true
                    }
                }
                ItemType.UNKNOWN_ANSWER -> {
                    binding.unknownAnswerTextView.text = item.text
                    binding.bugView.isVisible = true
                }
                ItemType.FIRST_ITEM -> {
                    binding.itemFirst.isVisible = true
                    binding.settingsIcon.setOnClickListener {
                        itemView.context.startActivity(
                            Intent(
                                itemView.context,
                                SettingsActivity::class.java
                            )
                        )
                    }
                    if (item.text != "FirstItem") {
                        binding.itemFirst.isGone = true
                    } else {
                        binding.itemFirst.isVisible = true
                    }
                }
                ItemType.TOGGLE_BLUETOOTH_OFF -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        binding.toggleCard.isVisible = true
                        binding.toggleTextView.text = item.text
                        binding.toggleView.isActivated = true
                        binding.toggleView.setOnCheckedChangeListener(
                            checkChangedListener(
                                itemView.context,
                                item.type
                            )
                        )
                    } else {
                        binding.answerTextView.text = item.text
                        binding.answerTextView.isVisible = true
                    }
                }
                ItemType.TOGGLE_BLUETOOTH_ON -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        binding.toggleCard.isVisible = true
                        binding.toggleTextView.text = item.text
                        binding.toggleView.isActivated = false
                        binding.toggleView.setOnCheckedChangeListener(
                            checkChangedListener(
                                itemView.context,
                                item.type
                            )
                        )
                    } else {
                        binding.answerTextView.text = item.text
                        binding.answerTextView.isVisible = true
                    }
                }
                ItemType.TOGGLE_WIFI_ON -> {
                    binding.toggleCard.isVisible = true
                    binding.toggleTextView.text = item.text
                    binding.toggleView.isActivated = false
                    binding.toggleView.setOnCheckedChangeListener(
                        checkChangedListener(
                            itemView.context,
                            item.type
                        )
                    )
                }
                ItemType.TOGGLE_WIFI_OFF -> {
                    binding.toggleCard.isVisible = true
                    binding.toggleTextView.text = item.text
                    binding.toggleView.isActivated = true
                    binding.toggleView.setOnCheckedChangeListener(
                        checkChangedListener(
                            itemView.context,
                            item.type
                        )
                    )
                }
                ItemType.TOGGLE_TORCH_ON -> {
                    binding.toggleCard.isVisible = true
                    binding.toggleTextView.text = item.text
                    binding.toggleView.isActivated = false
                    binding.toggleView.setOnCheckedChangeListener(
                        checkChangedListener(
                            itemView.context,
                            item.type
                        )
                    )
                }
                ItemType.TOGGLE_TORCH_OFF -> {
                    binding.toggleCard.isVisible = true
                    binding.toggleTextView.text = item.text
                    binding.toggleView.isActivated = true
                    binding.toggleView.setOnCheckedChangeListener(
                        checkChangedListener(
                            itemView.context,
                            item.type
                        )
                    )
                }

                ItemType.DATE -> {
                    binding.timeTextView.text = item.text
                    binding.timeCard.isVisible = true
                }

                ItemType.BIRTHDAY -> {
                    binding.birthdayCard.isVisible = true
                    binding.birthdayGreetingTextView.text = item.text
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun submitItem(item: Item): Int {
        items.add(item)
        notifyDataSetChanged()
        return items.size-1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    data class Item(
        var type: ItemType,
        var text: String,
        var drawable: Drawable? = null,
        var action: String = ""
    )

    enum class ItemType {
        QUESTION,
        ANSWER,
        FIRST_ITEM,
        UNKNOWN_ANSWER,
        TOGGLE_TORCH_ON,
        TOGGLE_TORCH_OFF,
        TOGGLE_BLUETOOTH_ON,
        TOGGLE_BLUETOOTH_OFF,
        TOGGLE_WIFI_ON,
        TOGGLE_WIFI_OFF,
        DATE,
        BIRTHDAY
        ;
    }

}
