package io.github.takusan23.dougasyusyusya.Service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.database.CursorIndexOutOfBoundsException
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import io.github.takusan23.dougasyusyusya.DataClass.VideoDataClass
import io.github.takusan23.dougasyusyusya.R
import io.github.takusan23.dougasyusyusya.Tool.MediaStoreTool
import kotlinx.coroutines.*
import java.util.*

class VideoMediaBrowserService : MediaBrowserServiceCompat() {

    /** [onLoadChildren]でparentIdに入ってくる。Android 11のメディアの再開の場合はこの値 */
    private val ROOT_RECENT = "root_recent"

    /** [onLoadChildren]でparentIdに入ってくる。[ROOT_RECENT]以外の場合 */
    private val ROOT = "root"

    /** 設定 */
    private val prefSettings by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    /** 音楽再生のExoPlayer */
    val exoPlayer by lazy { SimpleExoPlayer.Builder(this).build() }

    /** MediaSession */
    lateinit var mediaSessionCompat: MediaSessionCompat

    /** 通知出すのに使う */
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    /** 読み込んだ曲リスト */
    private val playList = arrayListOf<VideoDataClass>()

    /** 最初の動画配列 */
    private val initVideoIdList = arrayListOf<Long>()

    /** コルーチンの引数に使うやつ。サービス終了と同時にコルーチンも終了させるために使う */
    private val coroutineContext = Job() + Dispatchers.Main

    private val callback = object : MediaSessionCompat.Callback() {

        /** 再生準備 */
        override fun onPrepare() {
            super.onPrepare()

            GlobalScope.launch(coroutineContext) {
                // 動画読み込む
                val mediaItemList = arrayListOf<MediaItem>()
                // その他も
                playList.clear()
                playList.addAll(MediaStoreTool.getVideoList(this@VideoMediaBrowserService))
                playList.forEach { video ->
                    val mediaItem = MediaItem.Builder().apply {
                        setMediaId(video.id.toString())
                        setUri(video.uri)
                        setTag(video) // 動画情報詰めとく
                    }.build()
                    mediaItemList.add(mediaItem)
                }

                // IDだけ控える
                initVideoIdList.addAll(playList.map { data -> data.id })

                // 中身空っぽなら起動しない
                if (mediaItemList.isNotEmpty()) {
                    // ExoPlayerへ
                    exoPlayer.setMediaItems(mediaItemList)
                    exoPlayer.prepare()

                    // 最後に聞いた曲を探す。Android 11のMediaResumeのことだよ
                    val lastPlayMediaId = prefSettings.getLong("last_play_id", -1)
                    if (lastPlayMediaId != -1L) {
                        val video = MediaStoreTool.getVideo(this@VideoMediaBrowserService, lastPlayMediaId)
                        // 最後に聞いてた位置へ移動する
                        val pos = playList.indexOfFirst { videoDataClass -> videoDataClass.id == video.id }
                        if (pos != -1) {
                            // ない可能性を考慮
                            exoPlayer.seekTo(pos, 0)
                        }
                    }
                }

                // 曲一覧をクライアントへ送信
                updateQueueItemList()
            }

        }

        /** 再生 */
        override fun onPlay() {
            super.onPlay()
            startThisService()
            exoPlayer.playWhenReady = true
            mediaSessionCompat.isActive = true
        }

        /** MediaStoreのIdから再生 */
        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            super.onPlayFromMediaId(mediaId, extras)
            mediaId ?: return
            val pos = playList.indexOfFirst { videoDataClass -> videoDataClass.id.toString() == mediaId }
            if (pos != -1) {
                exoPlayer.seekTo(pos, 0)
            }
            exoPlayer.playWhenReady = true
        }

        /** 一時停止 */
        override fun onPause() {
            super.onPause()
            exoPlayer.playWhenReady = false
        }

        /** 止めた時 */
        override fun onStop() {
            super.onStop()
            mediaSessionCompat.isActive = false
            stopSelf()
        }

        /** 通知のシーク動かした時 */
        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            exoPlayer.seekTo(pos)
        }

        /** 前の曲 */
        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            exoPlayer.previous()
        }

        /** 次の曲 */
        override fun onSkipToNext() {
            super.onSkipToNext()
            exoPlayer.next()
        }

        /** リピートモード変更 */
        override fun onSetRepeatMode(repeatMode: Int) {
            super.onSetRepeatMode(repeatMode)
            // もしかして：この一行必須？
            mediaSessionCompat.setRepeatMode(repeatMode)

            if (repeatMode == PlaybackStateCompat.REPEAT_MODE_ALL) {
                // 無限ループループする
                exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
            } else {
                // 同じ曲を何回も聞く。
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            }
        }

        /** シャッフルモード変更 */
        override fun onSetShuffleMode(shuffleMode: Int) {
            super.onSetShuffleMode(shuffleMode)
            // もしかして：この一行必須？
            mediaSessionCompat.setShuffleMode(shuffleMode)

            val isShuffleMode = shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL
            if (isShuffleMode) {
                // シャッフルモード有効
                exoPlayer.clearMediaItems()
                playList.shuffle()
            } else {
                // シャッフルもとに戻す
                exoPlayer.clearMediaItems()
                playList.sortWith { a, b -> initVideoIdList.indexOf(a.id) - initVideoIdList.indexOf(b.id) }
            }
            // ExoPlayerへ追加
            playList.forEach { video ->
                val mediaItem = MediaItem.Builder().apply {
                    setMediaId(video.id.toString())
                    setUri(video.uri)
                    setTag(video) // 動画情報詰めとく
                }.build()
                exoPlayer.addMediaItem(mediaItem)
            }
            // クライアントへ返却
            mediaSessionCompat.setQueue(playList.map { videoDataClass ->
                MediaSessionCompat.QueueItem(createMediaDescriptionCompat(videoDataClass.id.toString(), videoDataClass.title, videoDataClass.title), videoDataClass.id)
            })

            // これひっす
            exoPlayer.prepare()
            exoPlayer.seekTo(0, 0)

        }

        /** 曲の移動 */
        override fun onCustomAction(action: String?, extras: Bundle?) {
            super.onCustomAction(action, extras)
            if (action == "move") {

                // 移動
                val from = extras?.getInt("from") ?: return
                val to = extras.getInt("to")

                // ExoPlayerでも移動ができるようになった
                exoPlayer.moveMediaItem(from, to)

                // 配列更新
                playList.clear()
                repeat(exoPlayer.mediaItemCount) { i ->
                    val item = exoPlayer.getMediaItemAt(i).playbackProperties?.tag as VideoDataClass
                    playList.add(item)
                }

                // キューに追加
                mediaSessionCompat.setQueue(playList.map { videoDataClass ->
                    MediaSessionCompat.QueueItem(createMediaDescriptionCompat(videoDataClass.id.toString(), videoDataClass.title, videoDataClass.title), videoDataClass.id)
                })
            }
        }

    }

    override fun onCreate() {
        super.onCreate()

        mediaSessionCompat = MediaSessionCompat(this, "douga_syu_syu_sya_media_session")
        sessionToken = mediaSessionCompat.sessionToken
        // MediaSessionの操作のコールバック
        mediaSessionCompat.setCallback(callback)
        // でふぉで全曲ループ
        mediaSessionCompat.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)

        // ExoPlayerの再生状態が更新されたときも通知を更新する
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, reason)
                updateState()
                showNotification()
            }

            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                updateState()
                showNotification()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                updateState()
                showNotification()
            }

        })

    }


    /**
     * クライアントへ曲一覧を返すときに使う関数
     *
     * クライアント側の[android.support.v4.media.session.MediaControllerCompat.Callback.onQueueChanged]が呼ばれる
     * */
    private fun updateQueueItemList() {
        mediaSessionCompat.setQueue(playList.mapIndexed { index, videoDataClass ->
            MediaSessionCompat.QueueItem(createMediaDescriptionCompat(videoDataClass.id.toString(), videoDataClass.title, videoDataClass.title), videoDataClass.id)
        })
    }

    /**
     * 再生状態とメタデータを設定する。今回はメタデータはハードコートする
     *
     * MediaSessionのsetCallBackで扱う操作([MediaSessionCompat.Callback.onPlay]など)も[PlaybackStateCompat.Builder.setState]に書かないと何も起きない
     * */
    private fun updateState() {
        // 再生中可能ではないならreturn
        exoPlayer.currentMediaItem ?: return

        val stateBuilder = PlaybackStateCompat.Builder().apply {
            // 取り扱う操作。
            setActions(
                PlaybackStateCompat.ACTION_PREPARE
                        or PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_STOP
                        or PlaybackStateCompat.ACTION_SEEK_TO
                        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        or PlaybackStateCompat.ACTION_SET_REPEAT_MODE
                        or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
            )
            /**
             * 曲の移動用に書いた。
             * Bundleにfrom、toを入れてね。
             * Queueの更新はありません
             * */
            addCustomAction("move", "move", R.drawable.ic_outline_audiotrack_24)
            // 再生してるか。ExoPlayerを参照
            val state = if (exoPlayer.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
            // 位置
            val position = exoPlayer.currentPosition
            // 再生状態を更新
            setState(state, position, 1.0f) // 最後は再生速度
        }.build()
        mediaSessionCompat.setPlaybackState(stateBuilder)
        val video = exoPlayer.currentMediaItem?.playbackProperties?.tag as VideoDataClass
        // メタデータの設定
        val mediaMetadataCompat = MediaMetadataCompat.Builder().apply {
            // Android 11 の MediaSession で使われるやつ
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, video.title)
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, video.title)
            putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, MediaStoreTool.getThumb(this@VideoMediaBrowserService, video.id))
            putLong(MediaMetadataCompat.METADATA_KEY_DURATION, exoPlayer.duration) // これあるとAndroid 10でシーク使えます
        }.build()
        mediaSessionCompat.setMetadata(mediaMetadataCompat)

        if (exoPlayer.isPlaying) {
            // 最後に聞いた曲を保存。ただしこの関数は再生中以外でもよばれるんで条件分岐が必要
            prefSettings.edit {
                putString("last_play_title", video.title)
                putLong("last_play_id", video.id)
                putString("last_play_artist", video.title)
            }
        }
    }

    /** 通知を表示する */
    private fun showNotification() {
        // 再生中可能ではない + 再生ボタンを押していない 場合は return
        if (exoPlayer.currentMediaItem == null && !mediaSessionCompat.isActive) return

        // 通知を作成。通知チャンネルのせいで長い
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 通知チャンネル
            val channelId = "playlist_play"
            val notificationChannel = NotificationChannel(channelId, "音楽コントローラー", NotificationManager.IMPORTANCE_LOW)
            if (notificationManager.getNotificationChannel(channelId) == null) {
                // 登録
                notificationManager.createNotificationChannel(notificationChannel)
            }
            NotificationCompat.Builder(this, channelId)
        } else {
            NotificationCompat.Builder(this)
        }
        notification.apply {

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                // Android 11 からは MediaSession から値をもらってsetContentTextしてくれるけど10以前はしてくれないので
                mediaSessionCompat.controller.metadata?.apply {
                    getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART).apply {
                        setLargeIcon(this)
                    }
                    setContentTitle(getText(MediaMetadataCompat.METADATA_KEY_TITLE))
                    setContentText(getText(MediaMetadataCompat.METADATA_KEY_ARTIST))
                }
            }

            setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSessionCompat.sessionToken).setShowActionsInCompactView(1, 2, 3))
            setSmallIcon(R.drawable.syu_syu_sya)
            // 通知領域に置くボタン
            addAction(NotificationCompat.Action(R.drawable.ic_baseline_clear_24, "停止", MediaButtonReceiver.buildMediaButtonPendingIntent(this@VideoMediaBrowserService, PlaybackStateCompat.ACTION_STOP)))
            addAction(NotificationCompat.Action(R.drawable.ic_outline_skip_previous_24, "前の曲", MediaButtonReceiver.buildMediaButtonPendingIntent(this@VideoMediaBrowserService, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)))
            if (exoPlayer.isPlaying) {
                addAction(NotificationCompat.Action(R.drawable.ic_outline_pause_24, "一時停止", MediaButtonReceiver.buildMediaButtonPendingIntent(this@VideoMediaBrowserService, PlaybackStateCompat.ACTION_PAUSE)))
            } else {
                addAction(NotificationCompat.Action(R.drawable.ic_outline_play_arrow_24, "再生", MediaButtonReceiver.buildMediaButtonPendingIntent(this@VideoMediaBrowserService, PlaybackStateCompat.ACTION_PLAY)))
            }
            addAction(NotificationCompat.Action(R.drawable.ic_outline_skip_next_24, "次の曲", MediaButtonReceiver.buildMediaButtonPendingIntent(this@VideoMediaBrowserService, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)))
        }
        // 通知表示
        startForeground(84, notification.build())
    }

    /** フォアグラウンドサービスを起動する */
    private fun startThisService() {
        val playlistPlayServiceIntent = Intent(this, VideoMediaBrowserService::class.java)
        // 起動
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(playlistPlayServiceIntent)
        } else {
            startService(playlistPlayServiceIntent)
        }
    }

    /**
     * [MediaBrowserServiceCompat]へ接続しようとした時に呼ばれる
     * Android 11 のメディアの再開では重要になっている
     * */
    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        // 最後の曲をリクエストしている場合はtrue
        val isRequestRecentMusic = rootHints?.getBoolean(BrowserRoot.EXTRA_RECENT) ?: false
        // BrowserRootに入れる値を変える
        val rootPath = if (isRequestRecentMusic) ROOT_RECENT else ROOT
        return BrowserRoot(rootPath, null)
    }

    /**
     * Activityとかのクライアントへ曲一覧を返す
     * */
    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        // 保険。遅くなると怒られるぽい？
        result.detach()
        if (parentId == ROOT_RECENT) {
            // 最後に聞いた曲をリクエストしてる
            val lastPlayTitle = prefSettings.getString("last_play_title", "") ?: return
            val lastPlayMediaId = prefSettings.getLong("last_play_id", 0).toString()
            val lastPlayArtist = prefSettings.getString("last_play_artist", "") ?: return
            // 動画情報いれる
            result.sendResult(arrayListOf(createMediaItem(lastPlayMediaId, lastPlayTitle, lastPlayArtist)))
        } else {
            result.sendResult(mutableListOf())
        }
    }

    /**
     * [onLoadChildren]で返すアイテムを作成する
     * */
    private fun createMediaItem(videoId: String, title: String, subTitle: String): MediaBrowserCompat.MediaItem {
        return MediaBrowserCompat.MediaItem(createMediaDescriptionCompat(videoId, title, subTitle), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }

    /**
     * [android.support.v4.media.MediaBrowserCompat.MediaItem]とか[android.support.v4.media.session.MediaSessionCompat.QueueItem]で使う
     * [MediaDescriptionCompat]を返す関数
     * */
    private fun createMediaDescriptionCompat(videoId: String, title: String, subTitle: String): MediaDescriptionCompat {
        return MediaDescriptionCompat.Builder().apply {
            setTitle(title)
            setSubtitle(subTitle)
            setIconBitmap(MediaStoreTool.getThumb(this@VideoMediaBrowserService, videoId.toLong()))
            setMediaId(videoId)
        }.build()
    }

    /** お片付け */
    override fun onDestroy() {
        super.onDestroy()
        mediaSessionCompat.release()
        exoPlayer.release()
        coroutineContext.cancelChildren()
    }


}