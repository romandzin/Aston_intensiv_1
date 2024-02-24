package com.music.player

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.music.player.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val serviceIntent: Intent by lazy {
        Intent(this, MusicService::class.java)
    }
    private var musicService: MusicService? = null
    private lateinit var currentSong: Song
    private var createdService = MutableLiveData<Boolean>()
    private val mainViewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            musicService = (service as MusicService.LocalBinder).getService()
            createdService.postValue(true)
            refreshFullUI()

            musicService!!.isNotificationClicked.observe(this@MainActivity) {
                refreshFullUI()
            }
            musicService!!.isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        requestPermission()
    }

    override fun onStart() {
        super.onStart()
        bindService(serviceIntent, connection, 0)
    }

    private fun initView() {
        binding.nextButton.setOnClickListener {
            mainViewModel.getNextSong()
        }
        binding.previousButton.setOnClickListener {
            mainViewModel.getPreviousSong()
        }
        mainViewModel.currentSong.observe(this) { song ->
            currentSong = song
            binding.musicImage.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    song.songImage
                )
            )
            if (musicService != null) {
                musicService!!.playNew(song)
                binding.playStopButton.setImageDrawable(
                    AppCompatResources.getDrawable(
                        this,
                        R.drawable.ic_play_arrow
                    )
                )
            }
        }
        binding.playStopButton.setOnClickListener {
            playStopButtonClicked()
        }
    }

    private fun refreshFullUI() {
        refreshPlayStopButton()
        refreshImage()
        mainViewModel.refreshIndex(musicService?.currentIndex)
    }

    private fun refreshPlayStopButton() {
        if (musicService != null && musicService?.player?.isPlaying != false) {
            binding.playStopButton.setImageDrawable(
                AppCompatResources.getDrawable(
                    this@MainActivity,
                    R.drawable.ic_stop
                )
            )
        } else {
            binding.playStopButton.setImageDrawable(
                AppCompatResources.getDrawable(
                    this@MainActivity,
                    R.drawable.ic_play_arrow
                )
            )
        }
    }

    private fun refreshImage() {
        if (musicService?.currentSong != null) binding.musicImage.setImageDrawable(
            AppCompatResources.getDrawable(
                this@MainActivity,
                musicService!!.currentSong!!.songImage
            )
        )
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    private fun playStopButtonClicked() {
        if (musicService == null) createService(mainViewModel.musicList)
        createdService.observe(this) {
            if (it) startOrStopMusic()
        }
    }

    private fun startOrStopMusic() {
        if (musicService?.player?.isPlaying == false) {
            musicService?.playMusic()
            binding.playStopButton.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.ic_stop
                )
            )
        } else {
            musicService?.stopMusic()
            binding.playStopButton.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.ic_play_arrow
                )
            )
        }
    }

    private fun createService(songsList: ArrayList<Song>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else startService(serviceIntent)
        bindService(serviceIntent, connection, 0)
        createdService.observe(this) {
            if (!musicService?.player?.isPlaying!!) musicService?.initService(
                songsList,
                currentSong
            )
            mainViewModel.refreshIndex(musicService?.currentIndex)
        }
    }

    override fun onStop() {
        super.onStop()
        if (musicService?.player?.isPlaying != true) musicService?.stopSelf()
        musicService!!.isBound = false
        musicService = null
        unbindService(connection)
        createdService = MutableLiveData<Boolean>()
    }
}