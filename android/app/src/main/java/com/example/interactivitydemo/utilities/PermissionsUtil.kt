package com.example.interactivitydemo.utilities
//
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.interactivitydemo.fragments.PERMISSIONS

class PermissionsUtil() {

    private val permissions = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO) // required persmission for video conferencing

    fun requestPermissions(launcher: ActivityResultLauncher<Array<String>>) {
        launcher.launch(permissions)
    }

    fun checkResults(results: MutableMap<String, Boolean>) : Boolean { // returns false if any permission was denied
        for (permission in permissions) {
            if (results[permission] != true) {
                return false
            }
        }
        return true
    }
}