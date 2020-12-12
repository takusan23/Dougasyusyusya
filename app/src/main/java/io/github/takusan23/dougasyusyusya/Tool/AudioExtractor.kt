package io.github.takusan23.dougasyusyusya.Tool

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * mp4から音声を抜き出す。変換無しなのでMediaCodecは使わない
 *
 * @param context その名の通り
 * @param uri 変換元Uri
 * @param extension 拡張子。aac以外も行けるっぽい？。MediaCodecっていうかフォーマットよくわからん
 * @param videoTitle 変換後のファイル名
 * */
class AudioExtractor(private val context: Context, private val uri: Uri, private val videoTitle: String, private val extension: String = "aac") {

    /** 保存先 */
    private val resultAudioFilePath = File(context.getExternalFilesDir(null), "temp.${extension}").path

    /** ファイルを取り出す */
    private val mediaExtractor = MediaExtractor()

    /** 最終的にデータをここに流してファイルを作成する。MUXER_OUTPUT_MPEG_4 が有ってるのかは知らん */
    private val mediaMuxer = MediaMuxer(resultAudioFilePath, MUXER_OUTPUT_MPEG_4)

    /** バッファサイズ。知らんから1MB */
    private val BUFFER_SIZE = 1024 * 1024

    /** 多分音声トラックはMediaExtractorから見て1なので(0は映像) */
    private val AUDIO_TRACK_INDEX = 1

    /**
     * MP4からAACを取り出す。コルーチンで使ってね
     * */
    suspend fun extractor() = withContext(Dispatchers.Default) {
        // 取り出すファイル。Uriから指定できた
        mediaExtractor.setDataSource(context, uri, null)
        // 多分Track2が音楽ファイル。AviUtilみたいに音声と映像がそれぞれ分かれてるので選択
        val audioMediaFormat = mediaExtractor.getTrackFormat(AUDIO_TRACK_INDEX)
        // Extractorのデータを入れるはこ
        val byteBuffer = ByteBuffer.allocate(BUFFER_SIZE)
        // MediaMuxerへ音楽をこれから追加するから用意してねーって頼む
        val trackId = mediaMuxer.addTrack(audioMediaFormat)

        // バッファ情報。謎
        val info = MediaCodec.BufferInfo()

        // 音声トラックを選んで
        mediaExtractor.selectTrack(AUDIO_TRACK_INDEX)
        mediaMuxer.start()

        // データが無くなるまで回す
        while (true) {
            val offset = byteBuffer.arrayOffset()
            info.size = mediaExtractor.readSampleData(byteBuffer, offset)
            if (info.size < 0) {
                // もうデータ取得しきったのでループ抜ける
                break
            }
            // 書き込む
            info.presentationTimeUs = mediaExtractor.sampleTime
            info.flags = mediaExtractor.sampleFlags
            mediaMuxer.writeSampleData(trackId, byteBuffer, info)
            // 次のデータに進める（忘れてた）
            mediaExtractor.advance()
        }

        // ループ抜け
        destroy()

        // MediaStoreへ挿入
        moveMediaStore()
    }

    /** MediaStoreへ書き込む。moveなのは[Context.getExternalFilesDir]から移動させるため */
    private fun moveMediaStore() {
        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "${videoTitle}.${extension}")
            put(MediaStore.Audio.Media.TITLE, videoTitle)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/${extension}") // 多分いる
            put(MediaStore.Audio.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        }
        if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
            // Android 10！！！。MediaStoreへ追加してUriをもらい書き込む
            // ディレクトリを作る。この保存先指定がAndroid 10以降にしかない
            contentValues.put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/DougaSyuSyuSya")
            val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return
            // MediaStore経由で保存する
            val outputStream = context.contentResolver.openOutputStream(uri, "w")
            // コピー
            outputStream?.write(File(resultAudioFilePath).inputStream().readBytes()) // Kotlinでらくする
            outputStream?.close()
        } else {
            // Android 9。この時代は直接JavaのFile APIが使えた。のでFileに書き込んでからMediaStoreへ挿入する
            val videoFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "DougaSyuSyuSya")
            if (!videoFolder.exists()) {
                videoFolder.mkdir() // なければ作成
            }
            // 書き込む
            val videoFile = File(videoFolder, "${videoTitle}.${extension}")
            videoFile.outputStream().write(File(resultAudioFilePath).inputStream().readBytes())
            // MediaStoreへ差し込む
            context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return
        }
        // 元のファイルを消す
        File(resultAudioFilePath).delete()
    }

    /** 終了時に呼んでね */
    fun destroy() {
        mediaExtractor.release()
        mediaMuxer.stop()
        mediaMuxer.release()
    }

}