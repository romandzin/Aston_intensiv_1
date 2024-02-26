package com.music.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData


class MusicService : Service() {

    var player: MediaPlayer? = null
    private val binder = LocalBinder()
    var currentIndex = 0
    private lateinit var manager: NotificationManager
    private lateinit var notification: Notification
    var currentSong: Song? = null
    private var musicList: ArrayList<Song>? = null
    private lateinit var musicNotificationReceiver: MusicNotificationReceiver
    val isNotificationClicked = MutableLiveData<Boolean>()
    var isBound = false
    val deathTimer = object : CountDownTimer(10000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            if (isBound) this.cancel()
        }

        override fun onFinish() {
            stopSelf()
        }

    }

    override fun onCreate() {
        super.onCreate()
        prepareNotificationReceiver()
        prepareNotification()
        createPlayer(R.raw.song_1)
    }

    private fun createPlayer(songData: Int) {
        player = MediaPlayer.create(this, songData)
        player?.isLooping = true
        player?.setVolume(100F, 100F)
    }

    private fun prepareNotificationReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction("previous")
        intentFilter.addAction("next")
        intentFilter.addAction("play/pause")
        musicNotificationReceiver = MusicNotificationReceiver()
        ContextCompat.registerReceiver(
            this,
            musicNotificationReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    fun initService(songsList: ArrayList<Song>, song: Song) {
        musicList = songsList
        playNew(song)
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.stop()
        player?.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        unregisterReceiver(musicNotificationReceiver)
    }

    private fun prepareNotification() {
        var channelId = "1"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel(channelId, "my channel")
        }
        createNotification(channelId)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_LOW
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_SECRET
        manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(chan)
        return channelId
    }

    private fun createNotification(channelId: String) {
        val notificationBuilder = NotificationCompat.Builder(
            applicationContext, channelId
        )
        notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Your song")
            .addAction(R.drawable.ic_skip_previous, "Previous", generatePendingIntent("previous"))
            .addAction(R.drawable.ic_play_arrow, "Play/Pause", generatePendingIntent("play/pause"))
            .addAction(R.drawable.ic_skip_next, "Next", generatePendingIntent("next"))
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1, notification)
        }
    }

    private fun generatePendingIntent(action: String): PendingIntent {
        val intent = Intent()
        intent.action = action
        return PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun playMusic() {
        player?.start()
    }

    fun stopMusic() {
        player?.pause()
    }

    fun playNew(song: Song) {
        player?.pause()
        player?.release()
        currentSong = song
        currentIndex = musicList!!.indexOf(song)
        createPlayer(song.songFile)
    }

    inner class LocalBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class MusicNotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "previous" -> {
                    currentIndex -= 1
                    if (currentIndex != -1) {
                        playNew(musicList!![currentIndex])
                    } else currentIndex = 0
                }

                "play/pause" -> {
                    if (player?.isPlaying == true) {
                        stopMusic()
                        if (!isBound) deathTimer.start()
                    } else {
                        deathTimer.cancel()
                        playMusic()
                    }
                }

                "next" -> {
                    currentIndex += 1
                    if (currentIndex != musicList!!.size) {
                        playNew(musicList!![currentIndex])
                    } else currentIndex -= 1
                }
            }
            isNotificationClicked.postValue(true)
        }
    }
}