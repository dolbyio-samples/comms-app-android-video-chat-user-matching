package com.example.interactivitydemo.fragments

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.interactivitydemo.R
import com.example.interactivitydemo.RoomActivity
import com.example.interactivitydemo.adapters.ConferenceAdapter
import com.example.interactivitydemo.databinding.FragmentBrowseBinding
import com.example.interactivitydemo.models.ConferenceModel
import com.example.interactivitydemo.services.NetworkingService
import com.example.interactivitydemo.utilities.PermissionsUtil
import com.voxeet.promise.solve.ThenVoid
import com.voxeet.sdk.models.Conference


private const val BROWSE_TAG = "BrowseFragment"

/**
 * A simple [Fragment] subclass.
 * Use the [BrowseFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BrowseFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var role: String
    private lateinit var otherRole: String

    private lateinit var alias: String


    private lateinit var binding: FragmentBrowseBinding
    private lateinit var conferenceRv: RecyclerView
    private lateinit var refreshLayout: SwipeRefreshLayout
    private var conferences = mutableListOf<ConferenceModel>()

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var permissionsUtil: PermissionsUtil

    private lateinit var mService: NetworkingService
    private var mBound: Boolean = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as NetworkingService.NetworkingBinder
            mService = binder.getService()
            mBound = true

            handleRole()
            role = mService.getRole()
            otherRole = if (role.equals(resources.getStringArray(R.array.roles_array)[1])) resources.getStringArray(R.array.roles_array)[2] else resources.getStringArray(R.array.roles_array)[1]

            onRefresh()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initLauncher()
    }

    override fun onStart() {
        super.onStart()

        val serviceIntent = Intent(activity, NetworkingService::class.java)
        activity?.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()

        activity?.unbindService(connection)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // inflate the layout for this fragment
        binding = FragmentBrowseBinding.inflate(inflater, container, false)
        conferenceRv = binding.conferencesRv
        refreshLayout = binding.refreshLayout

        refreshLayout.apply { // set logic for swipe to refresh
            setOnRefreshListener(this@BrowseFragment)
            isRefreshing = true
        }

        activity?.let { context ->
            refreshLayout.setColorSchemeColors(
                    ContextCompat.getColor(context, R.color.dolby_blue),
                    ContextCompat.getColor(context, R.color.dolby_purple),
                    ContextCompat.getColor(context, R.color.dolby_pink))
        }

        permissionsUtil.requestPermissions(requestPermissionLauncher) // request permissions upfront

        return binding.root
    }

    override fun onRefresh() {
        getConferenceList()
    }

    private fun initLauncher() {
        permissionsUtil = PermissionsUtil()
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (permissionsUtil.checkResults(results)) {
                onRefresh()
            }
            else {
                // handle user denial
                makeToast("Please allow the necessary permissions for video conferencing.")
            }
        }
    }

    private fun handleRole() { // get role from service, then adjust prompt text as necessary
        role = mService.getRole()

        var fillString = ""
        if (role == "Being interviewed") {
            fillString = "being interviewed by"
        }
        else {
            fillString = "interviewing"
        }
        binding.prompt.text = getString(R.string.practice_prompt, fillString)
    }

    private fun getConferenceList() { // initiate call to get conference list
        if (mBound) {
            mService.getConferenceList(otherRole, ::displayConferences, ::errorFetching)
        }
    }

    private fun displayConferences(conferences: MutableList<ConferenceModel>) { // callback on successful call to server
        this.conferences.clear()
        this.conferences = conferences
        refreshLayout.isRefreshing = false

        setAdapter()

        if (conferences.size > 0) {
            binding.noRoomsError.visibility = View.INVISIBLE
        }
        else {
            binding.noRoomsError.visibility = View.VISIBLE
        }
    }

    private fun errorFetching(error: Throwable) { // error on server call
        refreshLayout.isRefreshing = false
        Log.e(BROWSE_TAG, "error fetching list of conferences from server ${error.printStackTrace()}")
        makeToast("Could not fetch list of ongoing rooms. Please check your connection or try again.")
    }

    private fun setAdapter() {
        activity?.let { activity ->
            val conferenceAdapter = ConferenceAdapter(conferences, activity) { conference ->
                fetchFromList(conference)
            }
            conferenceRv.adapter = conferenceAdapter
            conferenceRv.layoutManager = LinearLayoutManager(activity)
        }
    }

    private fun fetchFromList(conferenceObj: ConferenceModel) { // fetch conference using id after clicking on a conference model
        if (mBound) {
            val fetchPromise = mService.fetchConference(conferenceObj.id)
            fetchPromise.then(ThenVoid { conference ->
                if (mService.conferenceIsFull(conference)) {
                    // full error
                    makeToast("That room is full. Please try a different room.")
                }
                else {
                    // join
                    joiningConference(conference) // successful fetching, join conference
                }
            })
                    .error {
                        makeToast("There was a problem joining the room. Check your connection.")
                        Log.e(BROWSE_TAG, "error fetching conference with id ${it.printStackTrace()}")
                    }
        }
    }

    private fun joiningConference(conference: Conference) { // join conference and start video call activity
        mService.joinConference(conference).then(ThenVoid {
            // start activity
            val roomActivityIntent = Intent(activity, RoomActivity::class.java)
            startActivity(roomActivityIntent)
        })
                .error {
                    makeToast("There was a problem joining the room. Check your connection.")
                    Log.e(BROWSE_TAG, "error joining conference ${it.printStackTrace()}")
                }
    }

    private fun makeToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    }


    companion object {

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment BrowseFragment.
         */
        @JvmStatic
        fun newInstance() =
                BrowseFragment()
    }

}