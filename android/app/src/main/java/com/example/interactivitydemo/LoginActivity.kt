package com.example.interactivitydemo

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.interactivitydemo.databinding.ActivityLoginBinding
import com.example.interactivitydemo.services.NetworkingService
import com.voxeet.promise.solve.ErrorPromise
import com.voxeet.promise.solve.ThenVoid

private val LOGIN_TAG = "LoginActivity"

class LoginActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    lateinit var binding: ActivityLoginBinding // view binding

    lateinit var goButton: Button
    lateinit var nameText: TextView
    lateinit var spinner: Spinner
    lateinit var progress: ProgressBar

    var role: String = ""

    private lateinit var mService : NetworkingService
    private var mBound: Boolean = false

    private val connection = object : ServiceConnection { // used to bind to the NetworkingService to provide networking operations
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(LOGIN_TAG, "service connected")
            val binder = service as NetworkingService.NetworkingBinder
            mService = binder.getService()
            mBound = true

            mService.startTokenRequest()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBound = false
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { // for spinner menu selection
        view?.let {
            hideKeyboard(view)
        }
        role = parent?.getItemAtPosition(position).toString()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) { // clear selection
        role = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_InteractivityDemo)
        binding = ActivityLoginBinding.inflate(layoutInflater)

        setContentView(binding.root)

        spinner = binding.role // initialize spinner
        ArrayAdapter.createFromResource(this, R.array.roles_array, R.layout.spinner).also {
            arrayAdapter ->
            arrayAdapter.setDropDownViewResource(R.layout.spinner_item)
            spinner.adapter = arrayAdapter
        }
        spinner.onItemSelectedListener = this

        goButton = binding.go
        nameText = binding.name
        progress = binding.loadSession
        setListeners()
    }

    override fun onStart() { // bind to service
        super.onStart()
        val serviceIntent = Intent(this, NetworkingService::class.java)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() { // disconnect from service
        super.onStop()
        unbindService(connection)
        mBound = false
    }


    private fun setListeners() { // set on click listeners
        goButton.setOnClickListener {
            tryLogin()
        }
        nameText.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                hideKeyboard(v)
            }
        }
        spinner.setOnTouchListener(View.OnTouchListener { v, event ->
            hideKeyboard(v)
            v.performClick()
            return@OnTouchListener false
        })
    }

    private fun tryLogin() {
        if (!mBound) { // if service is not bound, we don't have networking operations
            return
        }

        if (!mService.getInitializedStatus()) { // if SDK hasn't been initialized yet, can't start session
            makeToast("App not initialized yet. Please wait a moment or check your app key and secret for correctness.")
            mService.startTokenRequest()
            return
        }

        if (!role.isEmpty() && !role.equals(resources.getStringArray(R.array.roles_array).get(0)) && !nameText.text.isEmpty()) {

            progress.bringToFront()
            progress.visibility = View.VISIBLE

            mService.openSession(nameText.text.toString(), role)
                    .then(ThenVoid<Boolean> {
                        progress.visibility = View.INVISIBLE

                        // start activity
                        val mainActivityIntent = Intent(this, MainActivity::class.java)
                        startActivity(mainActivityIntent)
                    })
                    .error(ErrorPromise {
                        progress.visibility = View.INVISIBLE

                        Log.e(LOGIN_TAG, "error opening session + ${it.printStackTrace()}")
                        makeToast("Could not start app. Check your connection.")
                    })
        }
    }

    private fun hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0);
    }

    private fun makeToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() { // remove shared prefs to clear role and name settings
        super.onDestroy()
        val key = getString(R.string.preference_file_key)
        val sharedPrefs = getSharedPreferences(key, MODE_PRIVATE)
        with (sharedPrefs.edit()) {
            remove(key)
            clear()
            commit()
        }
    }

}