package io.github.takusan23.dougasyusyusya.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.github.takusan23.dougasyusyusya.DataClass.VideoDataClass
import io.github.takusan23.dougasyusyusya.Tool.MediaStoreTool

/**
 * [FileListFragment]のViewModel。
 *
 * */
class FileListFragmentViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    /** 動画配列 */
    val videoList = MutableLiveData(arrayListOf<VideoDataClass>())

    /** 曲開始LiveData */
    val selectVideo = MutableLiveData<VideoDataClass>()

    init {
        // 動画を取得する
        videoList.value = MediaStoreTool.getVideoList(context)
    }

    /** 動画を削除する */
    fun deleteVideo(videoDataClass: VideoDataClass) {
        MediaStoreTool.deleteMedia(context,videoDataClass.uri,videoDataClass.id)
    }

    fun startVideo(videoDataClass: VideoDataClass){

    }

}