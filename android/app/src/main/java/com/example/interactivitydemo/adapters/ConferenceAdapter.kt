package com.example.interactivitydemo.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.interactivitydemo.R
import com.example.interactivitydemo.databinding.ConferenceItemBinding
import com.example.interactivitydemo.models.ConferenceModel

class ConferenceAdapter(val conferences: MutableList<ConferenceModel>, val context: Context, val listener: (conferenceObj: ConferenceModel) -> Unit) : RecyclerView.Adapter<ConferenceAdapter.ViewHolder>(){

    class ViewHolder(view: View, binding: ConferenceItemBinding) : RecyclerView.ViewHolder(view) { // holds a view representing a conference

        val duration: TextView = binding.duration
        val dolbyVoice: ImageView = binding.dolbyVoice
        val confAlias: TextView = binding.conferenceName
        val owner: TextView = binding.ownerName
        val joinButton: Button = binding.joinButton

        fun bind(conference: ConferenceModel, context: Context) {
            duration.text = conference.getDurationInMinutes()
            dolbyVoice.backgroundTintList = ColorStateList.valueOf(if (conference.dolbyVoice) ContextCompat.getColor(context, R.color.dolby_blue) else ContextCompat.getColor(context, R.color.dolby_dark_gray))
            confAlias.text = conference.getAlias()
            owner.text = conference.getOwnerText()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ConferenceItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding.root, binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) { // set listener to be able to join by id when join button is clicked
        val conferenceItem = conferences.get(position)
        holder.bind(conferenceItem, context)
        holder.joinButton.setOnClickListener{
            listener(conferenceItem)
        }
    }

    override fun getItemCount(): Int {
        return conferences.size
    }
}