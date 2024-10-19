package com.palash.agora_video_calling

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.palash.agora_video_calling.databinding.ActivityMainBinding
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val appId = "f2d495dcc5904c70a7695285d712b930"
    private val channelName = "palashChannel"
    private val token =
        "007eJxTYOA49L816schoyrFrjwZyzzRK4e2nPvnbpd49+Xh+nXL9D4qMKQZpZhYmqYkJ5taGpgkmxskmptZmhpZmKaYGxolWRobRFgJpjcEMjJ8atdnZmSAQBCfl6EgMSexOMM5IzEvLzWHgQEA5J4jjg=="
    private val uid = 0

    private var isJoined = false
    private var agoraEngine: RtcEngine? = null
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null


    private val PERMISSION_ID = 22
    private val REQUESTED_PERMISSION = arrayOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CAMERA

    )

    private fun checkSelfPermission(): Boolean {
        return !(ContextCompat.checkSelfPermission(
            this,
            REQUESTED_PERMISSION[0]
        ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            REQUESTED_PERMISSION[1]
        ) != PackageManager.PERMISSION_GRANTED)
    }

    private fun showMessage(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setUpVideoSdkEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            agoraEngine!!.enableVideo()
        } catch (e: Exception) {
            showMessage(e.message.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep the screen awake
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSION, PERMISSION_ID)
        }

        setUpVideoSdkEngine()

        binding.joinButton.setOnClickListener {
            joinCall()
        }

        binding.leaveButton.setOnClickListener {
            leaveCall()
        }
    }

    private fun leaveCall() {
        if (!isJoined) {
            showMessage("Join a channel first")
        } else {
            agoraEngine!!.leaveChannel()
            showMessage("You left the channel")
            if (remoteSurfaceView != null)
                remoteSurfaceView!!.visibility = GONE
            if (localSurfaceView != null)
                localSurfaceView!!.visibility = GONE

            isJoined = false
        }
    }

    private fun joinCall() {
        if (checkSelfPermission()) {
            val option = ChannelMediaOptions()
            option.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            option.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            setupLocalVideo()
            localSurfaceView!!.visibility = VISIBLE
            agoraEngine!!.startPreview()
            agoraEngine!!.joinChannel(token, channelName, uid, option)
        } else {
            showMessage("Permission not grated")
        }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {

        override fun onUserJoined(uid: Int, elapsed: Int) {

            showMessage("Remote user joined $uid")
            runOnUiThread {
                setupRemoteVideo(uid)
            }

        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {

            isJoined = true
            showMessage("Join channel $channel")
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            showMessage("User offline $uid")
            runOnUiThread {
                remoteSurfaceView!!.visibility = GONE
            }
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        remoteSurfaceView = SurfaceView(baseContext)
        remoteSurfaceView!!.setZOrderMediaOverlay(true)
        binding.remoteUser.addView(remoteSurfaceView)

        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT, uid)
        )
    }

    private fun setupLocalVideo() {
        localSurfaceView = SurfaceView(baseContext)
        binding.localUser.addView(localSurfaceView)

        agoraEngine!!.setupLocalVideo(
            VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_FIT, 0)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine!!.stopPreview()
        agoraEngine!!.leaveChannel()

        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()

        // Optionally, remove the flag when the activity is destroyed
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}