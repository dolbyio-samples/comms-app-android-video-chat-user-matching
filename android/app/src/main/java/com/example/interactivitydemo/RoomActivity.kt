package com.example.interactivitydemo

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.interactivitydemo.databinding.ActivityRoomBinding
import com.example.interactivitydemo.services.NetworkingService
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.voxeet.VoxeetSDK
import com.voxeet.promise.solve.ThenVoid
import com.voxeet.sdk.events.v2.*
import com.voxeet.sdk.views.VideoView
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

private const val ROOM_ACTIVITY_TAG = "RoomActivity"


class RoomActivity : AppCompatActivity() {

    lateinit var binding: ActivityRoomBinding // view binding

    lateinit var streamLarge: VideoView
    lateinit var streamSmall: VideoView
    lateinit var hangUp: FloatingActionButton
    lateinit var muteAudio: FloatingActionButton
    lateinit var muteVideo: FloatingActionButton

    private var views = mutableListOf<View>() // for hiding or showing UI

    private lateinit var alias: String

    private lateinit var confName: TextView
    private lateinit var participantsTv: TextView

    var hasAudio: Boolean = true
    var hasVideo: Boolean = true

    private lateinit var mService: NetworkingService
    private var mBound: Boolean = false

    private val connection = object : ServiceConnection { // connect to service for networking calls
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as NetworkingService.NetworkingBinder
            mService = binder.getService()
            mBound = true

            alias = mService.getAlias()
            setText()

            // start video
            mService.startVideo()
                    .then(ThenVoid {
                        if (it) {
                            updateStreams()
                        }
                    })
                    .error {
                        Toast.makeText(this@RoomActivity, "Couldn't start video.", Toast.LENGTH_LONG).show()
                        Log.e(ROOM_ACTIVITY_TAG, "error starting video ${it.printStackTrace()}")
                    }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBound = false
        }
    }

    private val hideUIRunnable: Runnable = Runnable { // for hiding ui after period of no interaction
        hideUI()
    }

    private val hideUIHandler: Handler = Handler(Looper.getMainLooper())
    private var animationTime: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_InteractivityDemo)

        binding = ActivityRoomBinding.inflate(layoutInflater)

        setContentView(binding.root)

        streamLarge = binding.videoOther
        streamSmall = binding.videoSelf

        hangUp = binding.hangUp
        muteAudio = binding.muteAudio
        muteVideo = binding.muteVideo

        confName = binding.confName
        participantsTv = binding.participants

        views.add(hangUp)
        views.add(muteAudio)
        views.add(muteVideo)
        views.add(confName)
        views.add(participantsTv)
        views.add(binding.shadow)

        setListeners()
        setText()

        hideUIHandler.postDelayed(hideUIRunnable, 5000) // after five seconds of no interactions, hide UI
        animationTime = resources.getInteger(android.R.integer.config_shortAnimTime)
    }

    override fun onStart() { // bind to service
        super.onStart()
        val serviceIntent = Intent(this, NetworkingService::class.java)
        this.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() { // unbind
        super.onStop()

        this.unbindService(connection)
    }

    override fun onResume() {
        super.onResume()
        VoxeetSDK.instance().register(this)
    }

    override fun onPause() {
        super.onPause()
        VoxeetSDK.instance().unregister(this)
    }

    override fun onUserInteraction() { // reset timer to hide UI
        super.onUserInteraction()
        showUI()
        hideUIHandler.removeCallbacks(hideUIRunnable)
        hideUIHandler.postDelayed(hideUIRunnable, 5000)
    }

    private fun updateStreams() { // reattach or release video views
        if (mBound) {
            mService.updateStreams(streamSmall, streamLarge, ::setNoVideo)
        }
    }

    private fun hideUI() { // fade views out
        for (component in views) {
            if (component.visibility != View.INVISIBLE) {
                component.animate()
                        .alpha(0f)
                        .setDuration(animationTime.toLong())
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                component.visibility = View.GONE
                            }
                })
            }
        }
    }

    private fun showUI() { // fade views in
        for (component in views) {
            if (component.visibility != View.VISIBLE) {
                component.apply {
                    alpha = 0f
                    visibility = View.VISIBLE

                    animate()
                            .alpha(1f)
                            .setDuration(animationTime.toLong())
                            .setListener(null)
                }
            }
        }
    }

    private fun setNoVideo(videoView: VideoView) { // remove, then replace the videoview to show the background color when no video is streaming
        val index = binding.root.indexOfChild(videoView)
        binding.root.removeViewAt(index)
        binding.root.addView(videoView, index)
        videoView.setBackgroundResource(R.drawable.gradient)
    }

    fun setListeners() {
        hangUp.setOnClickListener {
            endCall()
        }
        muteAudio.setOnClickListener {
            toggleChecked(muteAudio)
            toggleAudio()
        }
        muteVideo.setOnClickListener {
            toggleChecked(muteVideo)
            toggleVideo()
        }
    }

    private fun setText() { // properly display room heading
        if (mBound) {
            if (alias.length < 24) {
                confName.text = alias
            } else {
                confName.text = alias.substring(0, 21).plus("...")
            }

            val participants = mService.getActiveParticipants()

            when (participants.size) {
                1 -> {
                    participantsTv.text = participants[0].info?.name ?: "User"
                }
                2 -> {
                    participants.apply {
                        participantsTv.text = getString(R.string.names_placeholder, this[0].info?.name.toString(), this[1].info?.name.toString())
                    }
                }
                else -> {
                    participantsTv.text = ""
                }
            }
        }
    }

    fun endCall() { // leave conference
        if (mBound) {
            mService.leaveConference()
                    .then(ThenVoid {
                        Log.d(ROOM_ACTIVITY_TAG, "successfully left conference")
                        streamLarge.release()
                        streamSmall.release()
                        finish()
                    })
                    .error {
                        Log.e(ROOM_ACTIVITY_TAG, "error leaving ${it}")
                        Toast.makeText(this, "There was a problem leaving the room.", Toast.LENGTH_LONG).show()
                    }
        }
    }

    override fun onBackPressed() {
        // don't exit call when back button is pressed
    }

    fun toggleChecked(button: FloatingActionButton) {
        var shouldBeChecked = true // true if button should indicate "on" state (e.g. muted or video off)

        if (button.id == R.id.mute_audio) {
            shouldBeChecked = if (hasAudio) true else false
        }
        if (button.id == R.id.mute_video) {
            shouldBeChecked = if (hasVideo) true else false
        }

        if (shouldBeChecked) { // adjust UI
            button.background = ContextCompat.getDrawable(this, R.color.dolby_dark_gray)

            if (button.id == R.id.mute_audio) {
                button.setImageResource(R.drawable.ic_baseline_mic_off_24)
            }
            else {
                button.setImageResource(R.drawable.ic_baseline_videocam_off_24)
            }
        }
        else {
            if (button.id == R.id.mute_audio) {
                button.setImageResource(R.drawable.ic_baseline_mic_24)
            }
            else {
                button.setImageResource(R.drawable.ic_baseline_videocam_24)
            }
        }
    }

    fun toggleAudio() { // mute or unmute
        if (mBound) {
            mService.toggleAudio(hasAudio)
            hasAudio = !hasAudio
        }
    }

    fun toggleVideo() { // stop or start sending video
        if (mBound) {
            mService.toggleVideo(hasVideo)
                    .then(ThenVoid {
                        Log.d(ROOM_ACTIVITY_TAG, "video updated")
                    })
                    .error {
                        Log.e(ROOM_ACTIVITY_TAG, "video stream not updated ${it.printStackTrace()}")
                    }
            hasVideo = !hasVideo
        }
    }

    override fun onDestroy() {
        endCall()
        super.onDestroy()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onStreamAdded(event: StreamAddedEvent){
        Log.d(ROOM_ACTIVITY_TAG, "stream added")
        updateStreams()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onStreamUpdated(event: StreamUpdatedEvent){
        Log.d(ROOM_ACTIVITY_TAG, "stream updated")
        updateStreams()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onStreamRemoved(event: StreamRemovedEvent) {
        Log.d(ROOM_ACTIVITY_TAG, "stream removed")
        updateStreams()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onParticipantJoined(event: ParticipantAddedEvent) {
        setText()
        Log.d(ROOM_ACTIVITY_TAG, "participant joined, ${event.participant.info?.name}")
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onParticipantUpdated(event: ParticipantUpdatedEvent) {
        setText()
        Log.d(ROOM_ACTIVITY_TAG, "participant updated, ${event.participant.info?.name}")
    }


}