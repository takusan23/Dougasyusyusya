package io.github.takusan23.dougasyusyusya.ViewModel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.github.takusan23.dougasyusyusya.DataClass.VideoDataClass
import io.github.takusan23.dougasyusyusya.Tool.AudioExtractor
import io.github.takusan23.dougasyusyusya.Tool.MediaStoreTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [VideoListFragment]のViewModel。
 *
 * */
class ViewoListFragmentViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    /** 動画配列 */
    val videoList = MutableLiveData(arrayListOf<VideoDataClass>())

    /** [io.github.takusan23.dougasyusyusya.BottomFragment.VideoMenuBottomFragment]を閉じるLiveData。データ型は正直何でも良かった */
    val bottomFragmentCloseLiveData = MutableLiveData<String>()

    /** 動画読み込み開始LiveData */
    val loadingVideoList = MutableLiveData<Boolean>()

    /** こっから曲開始LiveData */
    val selectVideo = MutableLiveData<VideoDataClass>()

    init {
        viewModelScope.launch {
            // 動画を取得する
            getVideoList()
        }
    }

    /** Fragmentから動画一覧更新するため */
    fun loadVideoList() {
        viewModelScope.launch {
            getVideoList()
        }
    }

    /** 動画一覧を更新。LiveDataに飛ばします。コルーチンです */
    private suspend fun getVideoList() = withContext(Dispatchers.IO) {
        loadingVideoList.postValue(true)
        // 動画を取得する
        videoList.postValue(MediaStoreTool.getVideoList(context))
        loadingVideoList.postValue(false)
    }

    /** 動画を削除する */
    fun deleteVideo(videoDataClass: VideoDataClass) {
        MediaStoreTool.deleteMedia(context, videoDataClass.uri, videoDataClass.id)
        Toast.makeText(context, "削除したよ", Toast.LENGTH_SHORT).show()
        viewModelScope.launch {
            getVideoList()
        }
    }

    /** 音楽モードを指定の動画から再生する場合に利用する */
    fun startVideo(videoDataClass: VideoDataClass) {
        selectVideo.value = videoDataClass
    }

    fun videoToAudio(videoDataClass: VideoDataClass) {
        viewModelScope.launch {
            val extractor = AudioExtractor(context, videoDataClass.uri, videoDataClass.title, "aac")
            extractor.extractor()
            Toast.makeText(context, "AAC形式に変換しました", Toast.LENGTH_SHORT).show()
            bottomFragmentCloseLiveData.postValue("close")
        }
    }

}