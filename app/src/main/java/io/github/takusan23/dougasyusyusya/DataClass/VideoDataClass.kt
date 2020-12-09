package io.github.takusan23.dougasyusyusya.DataClass

import android.graphics.Bitmap
import android.net.Uri

/**
 * 動画のデータクラス。
 *
 * 音楽再生とかで使ってる？
 * */
data class VideoDataClass(
    val title: String,
    val id: Long,
    val uri: Uri,
    val thumb: Bitmap?
)