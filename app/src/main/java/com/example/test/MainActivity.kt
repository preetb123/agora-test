package com.example.test

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.test.databinding.ActivityMainBinding
import io.agora.rtc2.*
import io.agora.rtm.*


class MainActivity : AppCompatActivity() {

    private var rtmChannel: RtmChannel? = null;
    private var rtmTokenString: String? = null;

    // Fill the App ID of your project generated on Agora Console.
    private val appId = "9b82a4a719e24fe280fe652fe06f2f6b"


    // An integer that identifies the local user.
    private var uid: Int = 0;

    // Track the status of your connection
    private var isJoined = false
    private var isMuted = false

    // Agora engine instance
    private var agoraEngine: RtcEngine? = null
    private var rtmClient: RtmClient? = null;

    // UI elements
    private var infoText: TextView? = null
    private var joinLeaveButton: Button? = null

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Call Sample"


        // If all the permissions are granted, initialize the RtcEngine object and join a channel.
        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        }

        setupVoiceSDKEngine();

        // Set up access to the UI elements
        joinLeaveButton = findViewById(R.id.joinLeaveButton);
        infoText = findViewById(R.id.infoText);

        binding.muteUnmuteUser.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked) {
                agoraEngine?.muteLocalAudioStream(true)
                binding.muteUnmuteCheck.setText("Unmute")
            }else{
                agoraEngine?.muteLocalAudioStream(false)
                binding.muteUnmuteCheck.setText("Mute")
            }
        }

        val builder = AlertDialog.Builder(this)
        builder.setMessage("Enter user id")
        val input = EditText(this)

        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        mapConnectionTypes()

        // Set up the buttons
        // Set up the buttons
        builder.setPositiveButton(
            "OK"
        ) { dialog, which ->
            uid = Integer.parseInt(input.text.toString())
            if(uid == 12345){
                rtmTokenString = "0069b82a4a719e24fe280fe652fe06f2f6bIACJz6S5UobWPbFBYsw8qIvl8MovfJkMh0rtskozB6CewBw69csAAAAAEADW3RyCQ0xHZAEA6ANDTEdk"
            }else if(uid == 67890){
                rtmTokenString = "0069b82a4a719e24fe280fe652fe06f2f6bIACgHHma4IttqfY4FBmxdLw9IWjCHBN7N0RpofsLH9XtnB1zmocAAAAAEADW3RyCM0xHZAEA6AMzTEdk"
            }else if(uid == 45678){
                rtmTokenString = "0069b82a4a719e24fe280fe652fe06f2f6bIAAJmtSCNX83e00G0JmtisYVi6FOBsJ663VhTZU3agHhoEBglc0AAAAAEADW3RyCIkxHZAEA6AMiTEdk"
            }else if(uid == 88888){
                rtmTokenString = "0069b82a4a719e24fe280fe652fe06f2f6bIAC4/lmAwcznPaoH1r3FUw5T2OgcDmXtV0GcN62t0CFrq1lkCXcAAAAAEADW3RyC5ktHZAEA6APmS0dk"
            }
        }

        builder.create().show()
    }

    private fun mapConnectionTypes() {

    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine!!.leaveChannel()

        // Destroy the engine in a sub-thread to avoid congestion
        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }

    var mapConnectionState: HashMap<Int?, String?> = object : HashMap<Int?, String?>() {
        init {
            put(1, "CONNECTION_STATE_DISCONNECTED")
            put(2, "CONNECTION_STATE_CONNECTING")
            put(3, "CONNECTION_STATE_CONNECTED")
            put(4, "CONNECTION_STATE_RECONNECTING")
            put(5, "CONNECTION_STATE_ABORTED")
        }
    }

    var mapConnectionStateChangeRason: HashMap<Int?, String?> = object : HashMap<Int?, String?>() {
        init {
            put(1, "CONNECTION_CHANGE_REASON_LOGIN")
            put(2, "CONNECTION_CHANGE_REASON_LOGIN_SUCCESS")
            put(3, "CONNECTION_CHANGE_REASON_LOGIN_FAILURE")
            put(4, "CONNECTION_CHANGE_REASON_LOGIN_TIMEOUT")
            put(5, "CONNECTION_CHANGE_REASON_INTERRUPTED")
            put(6, "CONNECTION_CHANGE_REASON_LOGOUT ")
            put(7, "CONNECTION_CHANGE_REASON_BANNED_BY_SERVER")
            put(8, "CONNECTION_CHANGE_REASON_REMOTE_LOGIN")
            put(9, "CONNECTION_CHANGE_REASON_TOKEN_EXPIRED ")
        }
    }

    private val TAG = "MainActivity"

    fun updateConnectionState(state: String){
        findViewById<TextView>(R.id.connectionstate).setText(state)
    }

    private fun setupVoiceSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            rtmClient = RtmClient.createInstance(this, appId, object : RtmClientListener {
                override fun onConnectionStateChanged(state: Int, reason: Int) {
                    Log.d(
                        TAG,
                        "onConnectionStateChanged() called with: state = $state ${mapConnectionState.get(state)}, reason = $reason ${mapConnectionStateChangeRason.get(reason)}"
                    )
                    runOnUiThread {
                        updateConnectionState("Connection state: " + mapConnectionState.get(state) + ", reason: " + mapConnectionStateChangeRason.get(reason) )
                    }
                }

                override fun onMessageReceived(rtmMessage: RtmMessage, peerId: String) {
                    Log.d(
                        TAG,
                        "onMessageReceived() called with: rtmMessage = $rtmMessage, peerId = $peerId"
                    )
//                    if (mListenerList.isEmpty()) {
//                        // If currently there is no callback to handle this
//                        // message, this message is unread yet. Here we also
//                        // take it as an offline message.
//                        mMessagePool.insertOfflineMessage(rtmMessage, peerId)
//                    } else {
//                        for (listener in mListenerList) {
//                            listener.onMessageReceived(rtmMessage, peerId)
//                        }
//                    }
                }

                override fun onImageMessageReceivedFromPeer(p0: RtmImageMessage?, p1: String?) {
                    TODO("Not yet implemented")
                }

                override fun onFileMessageReceivedFromPeer(p0: RtmFileMessage?, p1: String?) {
                    TODO("Not yet implemented")
                }

                override fun onMediaUploadingProgress(p0: RtmMediaOperationProgress?, p1: Long) {
                    TODO("Not yet implemented")
                }

                override fun onMediaDownloadingProgress(p0: RtmMediaOperationProgress?, p1: Long) {
                    TODO("Not yet implemented")
                }

                override fun onTokenExpired() {}
                override fun onTokenPrivilegeWillExpire() {}
                override fun onPeersOnlineStatusChanged(status: Map<String, Int>) {
                    Log.d(TAG, "onPeersOnlineStatusChanged() called with: status = $status")
                }

            })
        } catch (e: Exception) {
            throw RuntimeException("Check the error.")
        }
    }



    val map = LinkedHashMap<Int, String>()

    fun updateUsers(){
        var text = "Users joined"  + "\n"
        map.map {
            text += "\n" + it.key + " - " + it.value
        }
        runOnUiThread {
            findViewById<TextView>(R.id.users).setText(text)
        }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the remote user joining the channel.
        override fun onUserJoined(uid: Int, elapsed: Int) {
            map.put(uid, "online");
            runOnUiThread {
                infoText!!.text = "Remote user joined: $uid"
                Toast.makeText(this@MainActivity, "Remote user joined: $uid", Toast.LENGTH_SHORT).show()
                updateUsers()
            }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            // Successfully joined a channel
            isJoined = true
            map.put(uid, "online")

            showMessage("Joined Channel $channel")
            runOnUiThread {
                infoText!!.text = "Waiting for a remote user to join"
                joinLeaveButton?.isEnabled = true

                findViewById<RelativeLayout>(R.id.groupcallinfo).visibility = View.VISIBLE
                joinChannelButtonsEnabled(false)
                showLeaveChannelButton(true)
                updateUsers()
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            // Listen for remote users leaving the channel
            map.put(uid, "offline");
            showMessage("Remote user offline $uid $reason")
            updateUsers()
            if (isJoined) runOnUiThread { infoText!!.text = "Waiting for a remote user to join" }
        }

        override fun onLeaveChannel(stats: RtcStats) {
            // Listen for the local user leaving the channel
            runOnUiThread { infoText!!.text = "Press the button to join a channel" }
            isJoined = false
            map.clear()

            runOnUiThread(Runnable {
                Toast.makeText(this@MainActivity, "Remote user left: $uid", Toast.LENGTH_SHORT).show()
                joinChannelButtonsEnabled(true)
                showLeaveChannelButton(false)
                updateUsers()
            })
        }
    }

    fun joinChannelButtonsEnabled(callEnded: Boolean){
        findViewById<Button>(R.id.joinChannel2).isEnabled = callEnded
        findViewById<Button>(R.id.joinChannel3).isEnabled = callEnded
        findViewById<Button>(R.id.joinChannel1).isEnabled = callEnded
    }

    fun showLeaveChannelButton(shouldShow: Boolean){
        findViewById<LinearLayoutCompat>(R.id.layout).visibility = if(shouldShow) View.VISIBLE else View.GONE
    }

    private fun joinChannel(token: String, rtmToken: String, channelName: String, uid: Int) {
        val options = ChannelMediaOptions()
        options.autoSubscribeAudio = true
        // Set both clients as the BROADCASTER.
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        // Set the channel profile as BROADCASTING.
        options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING

        // Join the channel with a temp token.
        // You need to specify the user ID yourself, and ensure that it is unique in the channel.
        val res = agoraEngine!!.joinChannel(token, channelName, uid, options)
        if (res == 0) {
            Log.d("success", "call join success")

            rtmClient!!.login(rtmTokenString, uid.toString(), object : ResultCallback<Void?> {
                override fun onSuccess(responseInfo: Void?) {
                    Log.i(TAG, "login success")
                    runOnUiThread {

                    }
                    rtmChannel  = rtmClient!!.createChannel(channelName, object : RtmChannelListener {
                        override fun onMemberCountUpdated(i: Int) {}
                        override fun onAttributesUpdated(list: List<RtmChannelAttribute>) {}
                        override fun onMessageReceived(
                            rtmMessage: RtmMessage,
                            rtmChannelMember: RtmChannelMember
                        ) {
                            Log.d(
                                TAG,
                                "onMessageReceived() called with: rtmMessage = $rtmMessage, rtmChannelMember = $rtmChannelMember"
                            )
                        }

                        override fun onImageMessageReceived(
                            rtmImageMessage: RtmImageMessage,
                            rtmChannelMember: RtmChannelMember
                        ) {
                        }

                        override fun onFileMessageReceived(
                            rtmFileMessage: RtmFileMessage,
                            rtmChannelMember: RtmChannelMember
                        ) {
                        }

                        override fun onMemberJoined(rtmChannelMember: RtmChannelMember) {
                            Log.d(
                                TAG,
                                "onMemberJoined() called with: rtmChannelMember = $rtmChannelMember"
                            )
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "member joined: "+ rtmChannelMember.userId, Toast.LENGTH_SHORT).show()
                            }
                        }
                        override fun onMemberLeft(rtmChannelMember: RtmChannelMember) {
                            Log.d(
                                TAG,
                                "onMemberLeft() called with: rtmChannelMember = $rtmChannelMember"
                            )
                        }
                    })

                    if(rtmChannel != null){
                        Log.d(TAG, "rtmChannel joining = $responseInfo")
                        rtmChannel!!.join(object : ResultCallback<Void?> {
                            override fun onSuccess(responseInfo: Void?) {
                                println("rtm join channel success!")
                                runOnUiThread {
                                    Toast.makeText(this@MainActivity, "RTM Connected", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(errorInfo: ErrorInfo) {
                                System.out.println(
                                    "rtm join channel failure! errorCode = "
                                            + errorInfo.errorCode
                                )
                            }
                        })
                    }
                }

                override fun onFailure(errorInfo: ErrorInfo) {
                    Log.i(TAG, "login failed: " + errorInfo.errorCode)
                    runOnUiThread {
                    }
                }
            })

            findViewById<RelativeLayout>(R.id.groupcallinfo).visibility = View.VISIBLE
        }
        runOnUiThread {
            joinLeaveButton?.isEnabled = false
        }
    }

    fun joinLeaveChannel(view: View?) {
        if (isJoined) {
            agoraEngine!!.leaveChannel()
            joinLeaveButton!!.text = "Join"
            joinLeaveButton?.isEnabled = true
        } else {
            //joinChannel()
            joinLeaveButton!!.text = "Leave"
        }
    }

    fun joinChannel1(view: View?){
        joinChannel("channel1")
    }

    private fun joinChannel(channelName: String){
        TokenUtils.gen(this, channelName, uid, object: TokenUtils.OnTokenGenCallback {
            override fun onError(error: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error getting token: " + error.message, Toast.LENGTH_LONG).show()
                }

            }

            override fun onTokenGen(rtcToken: String, rtmToken: String) {
                joinChannel(rtcToken!!, rtmToken, "channel1", uid)
            }
        })
    }

    fun joinChannel2(view: View?){
        joinChannel("channel2")
    }

    fun leaveGroupCall(view: View?){
        agoraEngine!!.leaveChannel()
        if(rtmChannel != null){
            rtmChannel!!.leave(object : ResultCallback<Void?> {
                override fun onSuccess(responseInfo: Void?) {
                    println("rtm channel left successfully")
                    rtmChannel!!.release();

                    rtmClient!!.logout(object: ResultCallback<Void?> {
                        override fun onSuccess(p0: Void?) {
                            TODO("Not yet implemented")
                            rtmClient!!.release()
                        }

                        override fun onFailure(p0: ErrorInfo?) {
                            TODO("Not yet implemented")
                        }

                    })
                }

                override fun onFailure(errorInfo: ErrorInfo) {
                    System.out.println(
                        "join channel failure! errorCode = "
                                + errorInfo.errorCode
                    )
                }
            })
        }
        findViewById<RelativeLayout>(R.id.groupcallinfo).visibility = View.GONE
    }

    fun joinChannel3(view: View?){
        joinChannel("channel3")
    }

    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf<String>(
        Manifest.permission.RECORD_AUDIO
    )

    fun showMessage(message: String?) {
        runOnUiThread {
            Toast.makeText(
                applicationContext,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkSelfPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                REQUESTED_PERMISSIONS[0]
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            false
        } else true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return super.onSupportNavigateUp()
    }
}