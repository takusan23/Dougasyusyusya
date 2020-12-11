package io.github.takusan23.dougasyusyusya.Tool

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * mp4からaacを抜き出す。変換無しなのでMediaCodecは使わない
 * */
class AudioExtractor(val context: Context, val uri: Uri) {

    /** 保存先 */
    private val resultAudioFilePath = "${context.getExternalFilesDir(null)}/test.aac"

    /** ファイルを取り出す */
    private val mediaExtractor = MediaExtractor()

    /** 最終的にデータをここに流してファイルを作成する。aacはmp4の音声フォーマットの人一つなので */
    private val mediaMuxer = MediaMuxer(resultAudioFilePath, MUXER_OUTPUT_MPEG_4)

    /** バッファサイズ。知らんから取り出す1MB */
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
    }

    /** 終了時に呼んでね */
    fun destroy() {
        mediaExtractor.release()
        mediaMuxer.stop()
        mediaMuxer.release()
    }

}