package com.example.test

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
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


class MainActivity : AppCompatActivity() {

    // Fill the App ID of your project generated on Agora Console.
    private val appId = "9b82a4a719e24fe280fe652fe06f2f6b"


    // An integer that identifies the local user.
    private var uid: Int = 0;

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

        // Set up the buttons
        // Set up the buttons
        builder.setPositiveButton(
            "OK"
        ) { dialog, which -> uid = Integer.parseInt(input.text.toString()) }

        builder.create().show()
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

    val map = LinkedHashSet<Int>()

    fun updateUsers(){

        var text = "Users joined"
        map.forEach {
            text += "\n" + it
        }
        runOnUiThread {
            findViewById<TextView>(R.id.users).setText(text)
        }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the remote user joining the channel.
        override fun onUserJoined(uid: Int, elapsed: Int) {
            map.add(uid);
            runOnUiThread {
                infoText!!.text = "Remote user joined: $uid"
                Toast.makeText(this@MainActivity, "Remote user joined: $uid", Toast.LENGTH_SHORT).show()
                updateUsers()
            }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            // Successfully joined a channel
            isJoined = true
            map.add(uid)

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
            map.remove(uid);
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

    private fun joinChannel(token: String, channelName: String, uid: Int) {
        val options = ChannelMediaOptions()
        options.autoSubscribeAudio = true
        // Set both clients as the BROADCASTER.
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        // Set the channel profile as BROADCASTING.
        options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING

        // Join the channel with a temp token.
        // You need to specify the user ID yourself, and ensure that it is unique in the channel.
        val res = agoraEngine!!.joinChannel(token, channelName, uid, options)
        if (res != 0) {
            Log.d("success", "call join success")

            findViewById<RelativeLayout>(R.id.groupcallinfo).visibility = View.VISIBLE
        }
        joinLeaveButton?.isEnabled = false
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
        TokenUtils.gen(this, "channel1", uid, object: TokenUtils.OnTokenGenCallback<String> {
            override fun onTokenGen(ret: String?) {
                joinChannel(ret!!, "channel1", uid)
            }
        })
    }

    fun joinChannel2(view: View?){
        TokenUtils.gen(this, "channel2", uid, object: TokenUtils.OnTokenGenCallback<String> {
            override fun onTokenGen(ret: String?) {
                joinChannel(ret!!, "channel2", uid)
            }
        })
    }

    fun leaveGroupCall(view: View?){
        agoraEngine!!.leaveChannel()
        findViewById<RelativeLayout>(R.id.groupcallinfo).visibility = View.GONE
    }

    fun joinChannel3(view: View?){
        TokenUtils.gen(this, "channel3", uid, object: TokenUtils.OnTokenGenCallback<String> {
            override fun onTokenGen(ret: String?) {
                joinChannel(ret!!, "channel3", uid)
            }
        })
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