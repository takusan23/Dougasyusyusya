package io.github.takusan23.dougasyusyusya.ViewModel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.github.takusan23.dougasyusyusya.Service.DownloadService

/**
 * [io.github.takusan23.ytdl.Fragment.DownloadFragment]のViewModel
 *
 * UI関係ない実装はこっちに書いてると思う
 * */
class DownloadFragmentViewModel(application: Application) : AndroidViewModel(application) {

    val context = application.applicationContext

    val currentDownloadURL = MutableLiveData<String>()

    /**
     * ダウンロード開始関数
     * @param url URL
     * */
    fun download(url: String) {
        currentDownloadURL.postValue(url)
        // Service起動
        val intent = Intent(context, DownloadService::class.java).apply {
            putExtra("url", url)
        }
        context.startService(intent)
    }

    /**
     * youtube-dlのバイナリを更新する
     * */
    fun updateYoutubeDl() {
        // Service起動
        val intent = Intent(context, DownloadService::class.java).apply {
            putExtra("update", true)
        }
        context.startService(intent)
    }

}