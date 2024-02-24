package com.music.player

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private var currentIndex = 0

    val currentSong = MutableLiveData<Song>()

    val musicList = arrayListOf(
        Song(R.drawable.img_song_1, R.raw.song_1),
        Song(R.drawable.img_song_2, R.raw.song_2),
        Song(R.drawable.img_song_3, R.raw.song_3)
    )

    init {
        currentSong.postValue(musicList[0])
    }

    fun getNextSong() {
        currentIndex += 1
        if (currentIndex != musicList.size) {
            currentSong.postValue(musicList[currentIndex])
        } else currentIndex -= 1
    }

    fun getPreviousSong() {
        currentIndex -= 1
        if (currentIndex != -1) {
            currentSong.postValue(musicList[currentIndex])
        } else currentIndex = 0
    }

    fun refreshIndex(index: Int?) {
        currentIndex = index ?: 0
    }
}