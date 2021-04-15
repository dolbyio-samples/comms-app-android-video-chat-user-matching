package com.example.interactivitydemo.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.interactivitydemo.R
import com.example.interactivitydemo.constants.StringConstants
import com.example.interactivitydemo.models.ConferenceModel
import com.voxeet.VoxeetSDK
import com.voxeet.android.media.MediaStream
import com.voxeet.android.media.stream.MediaStreamType
import com.voxeet.promise.Promise
import com.voxeet.promise.PromiseInOut
import com.voxeet.promise.solve.ThenPromise
import com.voxeet.sdk.authent.token.RefreshTokenCallback
import com.voxeet.sdk.authent.token.TokenCallback
import com.voxeet.sdk.json.ParticipantInfo
import com.voxeet.sdk.json.internal.ParamsHolder
import com.voxeet.sdk.models.Conference
import com.voxeet.sdk.models.Participant
import com.voxeet.sdk.models.v1.ConferenceParticipantStatus
import com.voxeet.sdk.services.builders.ConferenceCreateOptions
import com.voxeet.sdk.services.builders.ConferenceJoinOptions
import com.voxeet.sdk.views.VideoView
import org.json.JSONArray
import org.json.JSONObject

class NetworkingService: Service() {

    private val binder = NetworkingBinder()

    private var initialized: Boolean = false
    private lateinit var name: String
    private lateinit var role: String

    inner class NetworkingBinder: Binder() { // gets role and name from shared preferences if exists, returns service to be bound to
        fun getService(): NetworkingService {
            val sharedPrefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

            if (sharedPrefs.contains(getString(R.string.saved_role_key))) { // if we've written to prefs before
                role = sharedPrefs.getString(getString(R.string.saved_role_key), "").toString()
                name = sharedPrefs.getString(getString(R.string.saved_name_key), "").toString()
                Log.d(SERVICE_TAG, "role $role, name $name")
            }

            return this@NetworkingService
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun startTokenRequest(callback: TokenCallback? = null) { // queue volley request for access token
        val stringRequest = StringRequest(Request.Method.GET, "${StringConstants.URL}/token/access",
                {
                    response ->
                        onTokenResponse(response, callback)
                },
                {
                    error ->
                        onTokenError(error, callback)
                })
        stringRequest.retryPolicy = DefaultRetryPolicy(TIMEOUT_MS, 3, 1F)
        val queue = Volley.newRequestQueue(this)
        queue.add(stringRequest)
    }

    private fun onTokenResponse(token: String, callback: TokenCallback?) { // response received from volley
        Log.d(SERVICE_TAG, "received token: $token")

        if (callback != null) { // refresh call
            Log.d(SERVICE_TAG, "successfully refreshed token")
            callback.ok(token)
        }
        else { // initial access token gotten
            VoxeetSDK.initialize(token, RefreshTokenCallback { isExpired, tokenCallback ->
                startTokenRequest(tokenCallback)
                if (isExpired) {
                    Log.d(SERVICE_TAG, "token expired")
                    // here, the application can show the user a pop up or similar saying there was a problem, and they will be disconnected
                }
            })
            initialized = true
        }
    }

    private fun onTokenError(error: VolleyError, callback: TokenCallback?) { // volley error while getting token
        Log.e(SERVICE_TAG, "error fetching token ${error.toString()}")
        callback?.error(error)
    }

    private fun writePrefs() { // persist name and role in local storage
        val sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putString(getString(R.string.saved_role_key), role)
            putString(getString(R.string.saved_name_key), name)
            apply()
        }
    }

    fun getInitializedStatus() : Boolean {
        return initialized
    }

    fun openSession(name: String, role: String): Promise<Boolean> { // given participant name, open session
        this.name = name
        this.role = role

        writePrefs()

        val participantInfo = ParticipantInfo(name, "", "")
        return VoxeetSDK.session().open(participantInfo)
    }

    fun createConference(conferenceAlias: String, useDolbyVoice: Boolean): PromiseInOut<Conference, Conference> { // create a conference
        val paramsHolder = ParamsHolder()
        paramsHolder.setDolbyVoice(useDolbyVoice)
        paramsHolder.setVideoCodec("VP8")

        val conferenceCreateOptions = ConferenceCreateOptions.Builder()
                .setConferenceAlias(conferenceAlias)
                .setParamsHolder(paramsHolder)
                .build()

        val createPromise = VoxeetSDK.conference().create(conferenceCreateOptions)

        val joinPromise = createPromise.then(ThenPromise<Conference, Conference> { conference ->
            val conferenceJoinOptions: ConferenceJoinOptions = ConferenceJoinOptions.Builder(conference).build()

            return@ThenPromise VoxeetSDK.conference().join(conferenceJoinOptions)
        })

        return joinPromise
    }

    fun checkConferenceExists(alias: String, callback: (exists: Boolean, error: Throwable?) -> Unit) { // to be called from activity before creating conference
        val params: MutableMap<String, String> = HashMap()
        params.put("alias", alias)

        val jsonObject = JSONObject()
        jsonObject.put("alias", alias) // server will search if active conference with this name exists

        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, "${StringConstants.URL}/conference/exists", jsonObject,
                { response ->
                    onExistsResponse(response, callback) // this callback is to be implemented by activity
                }, { error ->
                    onExistsError(error, callback)
        })
        val queue = Volley.newRequestQueue(this)
        queue.add(jsonObjectRequest)
    }

    private fun onExistsResponse(response: JSONObject, callback: (exists: Boolean, error: Throwable?) -> Unit) {
        val exists: Boolean = response["exists"] as Boolean // read json response into boolean variable

        callback(exists, null)
    }

    private fun onExistsError(error: VolleyError, callback: (exists: Boolean, error: Throwable?) -> Unit) {
        Log.e(SERVICE_TAG, "error checking if conference exists: ${error}")

        callback(false, error)
    }

    fun joinConference(conference: Conference): Promise<Conference> {
        val conferenceJoinOptions: ConferenceJoinOptions = ConferenceJoinOptions.Builder(conference).build()
        return VoxeetSDK.conference().join(conferenceJoinOptions)
    }

    fun fetchConference(confId: String): Promise<Conference> {
        return VoxeetSDK.conference().fetchConference(confId)
    }

    fun getConferenceList(otherRole: String, callback: (conferences: MutableList<ConferenceModel>) -> Unit, errorCallback: (error: Throwable) -> Unit) {
        val params: MutableMap<String, String> = HashMap()
        params["query"] = otherRole

        val jsonArray = JSONArray()
        jsonArray.put(JSONObject(params as Map<*, *>))

        val jsonObjectRequest = JsonArrayRequest(Request.Method.POST, "${StringConstants.URL}/conference/list/", jsonArray,
                { response ->
                    callback(parseConferences(response, otherRole)) // function to call if fetching list of confs succeeds
                },
                { error ->
                    errorCallback(error) // function to call if fetching list fails
        })
        val queue = Volley.newRequestQueue(this)
        queue.add(jsonObjectRequest)
    }

    private fun parseConferences(conferences: JSONArray, otherRole: String): MutableList<ConferenceModel> { // create conference model objects based on json response
        val parsedConferences = mutableListOf<ConferenceModel>()

        for (i in 0 until conferences.length()) {
            val conference : JSONObject = conferences.get(i) as JSONObject

            parsedConferences.add(ConferenceModel(
                    conference.getString("alias"),
                    conference.getString("confId"),
                    conference.getBoolean("dolbyVoice"),
                    conference.getInt("duration"),
                    conference.getString("owner"),
                    otherRole
            ))
        }

        return parsedConferences
    }

    fun conferenceIsFull(conference: Conference): Boolean { // checks for two active (non-left) participants
        return conference.participants.size > 1 && !conference.hasAny(ConferenceParticipantStatus.LEFT, false)
    }

    fun getAlias(): String { // returns prettified alias with prefix role removed if necessary
        val fullAlias = VoxeetSDK.conference().conference?.alias
        if (fullAlias != null) {
            val firstRole = resources.getStringArray(R.array.roles_array)[1]
            val secondRole = resources.getStringArray(R.array.roles_array)[2]
            if (fullAlias.startsWith(firstRole)) {
                return fullAlias.substring(firstRole.length)
            }
            else if (fullAlias.startsWith(secondRole)) {
                return fullAlias.substring(secondRole.length)
            }
            else {
                return fullAlias
            }
        }
        else {
            return ""
        }
    }

    fun leaveConference(): Promise<Boolean> {
        if (VoxeetSDK.conference().isInConference) {
            return VoxeetSDK.conference().leave()
        }
        else {
            return Promise.resolve(false)
        }
    }

    fun getActiveParticipants(): List<Participant> {
        val activeParticipants = mutableListOf<Participant>()
        for (participant in VoxeetSDK.conference().participants) {
            if (participant.status == null) {
                continue
            } else if (!participant.status.equals(ConferenceParticipantStatus.LEFT)) { // only count a participant if active
                activeParticipants.add(participant)
            }
        }

        return activeParticipants
    }

    fun startVideo(): Promise<Boolean> {
        VoxeetSDK.session().participant?.let { participant ->
            return VoxeetSDK.conference().startVideo(participant)
        }
        return Promise.resolve(false)
    }

    fun updateStreams(videoLocal: VideoView, videoRemote: VideoView, setNoVideoCallback: (videoView: VideoView) -> Unit) {
        for (participant in VoxeetSDK.conference().participants) {
            if (participant.status == ConferenceParticipantStatus.LEFT) {
                continue
            }
            val isLocal = VoxeetSDK.session().isLocalParticipant(participant)
            val videoView = if (isLocal) videoLocal else videoRemote

            val stream: MediaStream? = if (participant.streamsHandler().has(MediaStreamType.Camera)) participant.streamsHandler().getFirst(MediaStreamType.Camera) else null

            stream?.let { cameraStream ->
                if (cameraStream.videoTracks().isNotEmpty()) {
                    participant.id?.let { id ->
                        videoView.reinit()
                        videoView.attach(id, cameraStream)
                        if (isLocal) {
                            videoView.setMirror(true) // mirror local participant's video
                        }
                    }
                }
                else {
                    setNoVideoCallback(videoView)
                }
            }
            if (stream == null) { // camera is not on
                setNoVideoCallback(videoView) // implemented by the calling activity
            }
        }
    }

    fun toggleAudio(shouldMute: Boolean) {
        VoxeetSDK.conference().mute(shouldMute)
    }
    
    fun toggleVideo(shouldStop: Boolean): Promise<Boolean> {
        if (shouldStop) {
            return VoxeetSDK.conference().stopVideo()
        }
        else {
            return VoxeetSDK.conference().startVideo()
        }
    }

    fun closeSession(): Promise<Boolean> {
        return VoxeetSDK.session().close()
    }

    fun getLocalParticipant(): Participant? {
        return VoxeetSDK.session().participant
    }

    fun getName(): String {
        return this.name
    }

    fun getRole(): String {
        return this.role
    }

    companion object {
        const val SERVICE_TAG = "NetworkingService"
        const val TIMEOUT_MS = 3000
    }



}