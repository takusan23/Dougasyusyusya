package io.github.takusan23.dougasyusyusya.DataClass

import android.graphics.Bitmap
import android.net.Uri
import java.io.Serializable

/**
 * 動画のデータクラス。
 *
 * 音楽再生とかで使ってる？
 * */
data class VideoDataClass(
    val title: String,
    val fileName: String,
    val id: Long,
    val uri: Uri,
    val thumb: Bitmap?,
    val extension: String
) : Serializable