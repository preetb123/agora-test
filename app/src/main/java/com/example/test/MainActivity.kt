package com.example.test

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.test.databinding.ActivityMainBinding
import io.agora.rtc2.*


class MainActivity : AppCompatActivity() {

    // Fill the App ID of your project generated on Agora Console.
    private val appId = "9b82a4a719e24fe280fe652fe06f2f6b"

    // Fill the channel name.
    private val channelName = "sony-channel1"

    // Fill the temp token generated on Agora Console.
    private val token = "007eJxTYCjX/yXZnfrqn9W6A1fXnJRQWt6ycELwQeW/feIzb6fbr1ZVYLBMsjBKNEk0N7RMNTJJSzWyMEhLNTM1Sks1MEszSjNLkpHRS2kIZGRQ2O7BwAiFID4vQ3F+XqVuckZiXl5qjiEDAwDKeCK2"

    // An integer that identifies the local user.
    private val uid = 1234

    // Track the status of your connection
    private var isJoined = false
    private var isMuted = false

    // Agora engine instance
    private var agoraEngine: RtcEngine? = null

    // UI elements
    private var infoText: TextView? = null
    private var joinLeaveButton: Button? = null

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Call Sample"


        // If all the permissions are granted, initialize the RtcEngine object and join a channel.
        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        }

        setupVoiceSDKEngine();

        // Set up access to the UI elements
        joinLeaveButton = findViewById(R.id.joinLeaveButton);
        infoText = findViewById(R.id.infoText);

        binding.muteUnmuteCheck.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked) {
                agoraEngine?.muteLocalAudioStream(true)
                binding.muteUnmuteCheck.setText("Unmute")
            }else{
                agoraEngine?.muteLocalAudioStream(false)
                binding.muteUnmuteCheck.setText("Mute")
            }
        }
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

    private fun setupVoiceSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
        } catch (e: Exception) {
            throw RuntimeException("Check the error.")
        }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the remote user joining the channel.
        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread { infoText!!.text = "Remote user joined: $uid" }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            // Successfully joined a channel
            isJoined = true

            showMessage("Joined Channel $channel")
            runOnUiThread {
                infoText!!.text = "Waiting for a remote user to join"
                joinLeaveButton?.isEnabled = true
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            // Listen for remote users leaving the channel
            showMessage("Remote user offline $uid $reason")
            if (isJoined) runOnUiThread { infoText!!.text = "Waiting for a remote user to join" }
        }

        override fun onLeaveChannel(stats: RtcStats) {
            // Listen for the local user leaving the channel
            runOnUiThread { infoText!!.text = "Press the button to join a channel" }
            isJoined = false
        }
    }

    private fun joinChannel() {
        val options = ChannelMediaOptions()
        options.autoSubscribeAudio = true
        // Set both clients as the BROADCASTER.
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        // Set the channel profile as BROADCASTING.
        options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING

        // Join the channel with a temp token.
        // You need to specify the user ID yourself, and ensure that it is unique in the channel.
        agoraEngine!!.joinChannel(token, channelName, uid, options)

        joinLeaveButton?.isEnabled = false
    }

    fun joinLeaveChannel(view: View?) {
        if (isJoined) {
            agoraEngine!!.leaveChannel()
            joinLeaveButton!!.text = "Join"
            joinLeaveButton?.isEnabled = true
        } else {
            joinChannel()
            joinLeaveButton!!.text = "Leave"
        }
    }

    fun showContacts(view: View?){

    }

    fun showChannels(view: View){

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