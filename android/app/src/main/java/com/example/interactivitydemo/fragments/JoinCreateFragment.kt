package com.example.interactivitydemo.fragments

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.interactivitydemo.R
import com.example.interactivitydemo.RoomActivity
import com.example.interactivitydemo.databinding.FragmentJoinCreateBinding
import com.example.interactivitydemo.services.NetworkingService
import com.example.interactivitydemo.utilities.PermissionsUtil
import com.google.android.material.button.MaterialButton
import com.voxeet.promise.Promise
import com.voxeet.promise.solve.ThenVoid
import com.voxeet.sdk.models.Conference


private const val JOIN_FRAGMENT_TAG = "JoinCreateFragment"

val PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO)

/**
 * A simple [Fragment] subclass.
 * Use the [JoinCreateFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class JoinCreateFragment() : Fragment() {

    private lateinit var role: String

    private lateinit var binding: FragmentJoinCreateBinding

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>> // needed for requesting permissions
    private lateinit var permissionsUtil: PermissionsUtil

    private var private: Boolean = false
    private var useDolbyVoice: Boolean = false

    private var currButtonId: Int = 0

    private lateinit var alias: String

    private lateinit var mService: NetworkingService
    private var mBound: Boolean = false

    private val connection = object : ServiceConnection { // bind to service for networking operations
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as NetworkingService.NetworkingBinder
            mService = binder.getService()
            mBound = true

            role = mService.getRole()

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBound = false
        }
    }

    override fun onStart() {
        super.onStart()

        val serviceIntent = Intent(activity, NetworkingService::class.java)
        activity?.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()

        activity?.unbindService(connection)
        mBound = false
    }

    override fun onResume() {
        binding.joinButton.isEnabled = true
        binding.createButton.isEnabled = true
        super.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initLauncher()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // inflate the layout for this fragment
        binding = FragmentJoinCreateBinding.inflate(inflater, container, false)

        setListeners()

        return binding.root
    }

    private fun setListeners() {
        binding.joinButton.setOnClickListener {
            onEnter(binding.joinButton.id)
        }

        binding.createButton.setOnClickListener {
            onEnter(binding.createButton.id)
        }

        binding.privateSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            private = isChecked
        }

        binding.dlbyVoiceSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            useDolbyVoice = isChecked
        }

        binding.info.setOnClickListener {
            val infoDialogFragment = InfoDialogFragment()
            infoDialogFragment.show(childFragmentManager, "INFO")
        }

        binding.newId.addTextChangedListener(object: TextWatcher { // check for illegal characters or length
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let{ text ->
                    handleTextChange(binding.newIdError, binding.createButton, text.toString(), start, count)
                }
            }
        })

        binding.roomId.addTextChangedListener(object: TextWatcher { // check for illegal characters or length
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let{ text ->
                    handleTextChange(binding.idError, binding.joinButton, text.toString(), start, count)
                }
            }
        })
    }

    private fun onEnter(id: Int) {
        binding.joinButton.isEnabled = false
        binding.createButton.isEnabled = false

        if ((id == R.id.join_button && binding.roomId.text.isNotEmpty()) || (id == R.id.create_button && binding.newId.text.isNotEmpty())) {
            currButtonId = id
            getPermissions() // check for permissions first
        }
    }

    private fun tryJoin() {
        binding.loadConference.bringToFront()
        binding.loadConference.visibility = View.VISIBLE

        if (!mBound) {
            return
        }

        val callback = if (currButtonId == R.id.join_button) ::existsCallbackForJoin else ::existsCallbackForCreate

        alias = if (currButtonId == R.id.join_button) binding.roomId.text.toString() else binding.newId.text.toString()

        if (currButtonId == R.id.create_button && !private) {
            alias = aliasPrefix().plus(alias)
        }

        mService.checkConferenceExists(alias, callback) // kick off the joining process

    }

    private fun existsCallbackForJoin(exists: Boolean, error: Throwable?) { // check if conf exists and handle appropriately

        if (error != null) {
            // handle volley error
            Log.e(JOIN_FRAGMENT_TAG, "volley error checking if conference exists ${error.printStackTrace()}")
            binding.loadConference.visibility = View.INVISIBLE
            return
        }

        if (!exists) {
            makeToast("No room with that name could be found. Please try again.")
            binding.loadConference.visibility = View.INVISIBLE
            return
        }

        creatingConference()
    }

    private fun existsCallbackForCreate(exists: Boolean, error: Throwable?) { // check if conf exists and handle appropriately

        if (error != null) {
            // handle volley error
            Log.e(JOIN_FRAGMENT_TAG, "volley error checking if conference exists ${error.printStackTrace()}")
            binding.loadConference.visibility = View.INVISIBLE
        }
        if (exists) {
            makeToast("A room with that name already exists. Please pick a different name.")
            binding.loadConference.visibility = View.INVISIBLE
            return
        }

        creatingConference()

    }

    private fun creatingConference() { // makes call to service to create conference
        val createPromise = mService.createConference(alias, useDolbyVoice)

        createPromise
                .then<Promise<Conference>>(ThenVoid { conference ->
                    if (mService.conferenceIsFull(conference)) {
                        makeToast("The room you are trying to join is full. Please try a different room.")
                    }
                    else {
                        joiningConference(conference)
                    }
                })
                .error {
                    makeToast("Could not join the room. Check your connection or try again.")
                    binding.loadConference.visibility = View.INVISIBLE
                    Log.e(JOIN_FRAGMENT_TAG, "error while creating conf, ${it.printStackTrace()}")
                }
    }

    private fun joiningConference(conference: Conference) { // conference is not full -- join it and start activity

        val joinPromise = mService.joinConference(conference)

        joinPromise
                .then(ThenVoid { joinedConference ->
                    // start activity
                    val roomActivityIntent = Intent(activity, RoomActivity::class.java)
                    startActivity(roomActivityIntent)
                })
                .error {
                    makeToast("Could not join the room. Check your connection or try again.")
                    Log.e(JOIN_FRAGMENT_TAG, "error joining conference ${it.printStackTrace()}")
                }
        binding.loadConference.visibility = View.INVISIBLE
    }

    private fun handleTextChange(errorText: TextView, toDisable: MaterialButton, text: String, start: Int, count: Int) { // for input validation
        if (count == 1 && text[start].toString() == "#") {
            errorText.text = getString(R.string.id_error)
            errorText.visibility = View.VISIBLE
            toDisable.isEnabled = false
        }
        else if (text.length >= 250) {
            errorText.text = getString(R.string.id_error_length)
            errorText.visibility = View.VISIBLE
            toDisable.isEnabled = false
        }
        else {
            errorText.visibility = View.GONE
            toDisable.isEnabled = true
        }
    }

    private fun initLauncher() { // for permissions
        permissionsUtil = PermissionsUtil()
        // below will be called after user finishes interacting with permission dialogue
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (permissionsUtil.checkResults(results)) {
                tryJoin()
            }
            else {
                // handle user denial
                makeToast("Please allow the necessary permissions for video conferencing.")
                Log.e(JOIN_FRAGMENT_TAG, "user denied")
            }
        }
    }

    private fun getPermissions() { // launch permissions request
        permissionsUtil.requestPermissions(requestPermissionLauncher)
    }

    private fun aliasPrefix(): String { // returns role to be prepended to a public conference
        return if (role.equals(resources.getStringArray(R.array.roles_array)[1]))
            resources.getStringArray(R.array.roles_array)[1]
        else
            resources.getStringArray(R.array.roles_array)[2]
    }

    private fun makeToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param role the role chosen by the user at log in
         * @return A new instance of fragment JoinCreateFragment.
         */
        @JvmStatic
        fun newInstance() =
                JoinCreateFragment().apply {
                }
    }
}