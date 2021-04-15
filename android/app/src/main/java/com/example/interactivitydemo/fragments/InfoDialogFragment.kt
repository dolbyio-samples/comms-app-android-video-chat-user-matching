package com.example.interactivitydemo.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.interactivitydemo.R

class InfoDialogFragment: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setTitle(R.string.info_dialog_title)
                    .setMessage(R.string.info_dialog_message)
                    .setPositiveButton(R.string.info_dialog_button, 
                    DialogInterface.OnClickListener { dialog, which ->
                        dialog.dismiss()
                    })

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}