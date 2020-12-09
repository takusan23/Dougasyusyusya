package io.github.takusan23.dougasyusyusya.Service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
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
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import io.github.takusan23.dougasyusyusya.DataClass.VideoDataClass
import io.github.takusan23.dougasyusyusya.R
import io.github.takusan23.dougasyusyusya.Tool.MediaAccess

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

    override fun onCreate() {
        super.onCreate()

        exoPlayer.repeatMode = Player.REPEAT_MODE_ALL

        mediaSessionCompat = MediaSessionCompat(this, "douga_syu_syu_sya_media_session").apply {
            // MediaSessionの操作のコールバック
            setCallback(object : MediaSessionCompat.Callback() {

                /** 再生準備 */
                override fun onPrepare() {
                    super.onPrepare()
                    // 動画読み込む
                    val mediaItemList = arrayListOf<MediaItem>()
                    // 最後に聞いた曲を探す
                    val lastPlayMediaId = prefSettings.getLong("last_play_id", -1)
                    if (lastPlayMediaId != -1L) {
                        val video = MediaAccess.getVideo(this@VideoMediaBrowserService, lastPlayMediaId)
                        val mediaItem = MediaItem.Builder().apply {
                            setMediaId(video.id.toString())
                            setUri(video.uri)
                            setTag(video) // 動画情報詰めとく
                        }.build()
                        mediaItemList.add(mediaItem)
                    }
                    // その他も
                    MediaAccess.getVideoList(this@VideoMediaBrowserService).forEach { video ->
                        val mediaItem = MediaItem.Builder().apply {
                            setMediaId(video.id.toString())
                            setUri(video.uri)
                            setTag(video) // 動画情報詰めとく
                        }.build()
                        mediaItemList.add(mediaItem)
                    }
                    // 重複消す
                    mediaItemList.distinctBy { mediaItem -> mediaItem.mediaId }
                    // ExoPlayerへ
                    exoPlayer.setMediaItems(mediaItemList)
                    exoPlayer.prepare()
                }


                /** 再生 */
                override fun onPlay() {
                    super.onPlay()
                    startThisService()
                    exoPlayer.playWhenReady = true
                    isActive = true
                }

                /** 一時停止 */
                override fun onPause() {
                    super.onPause()
                    exoPlayer.playWhenReady = false
                }

                /** 通知のシーク動かした時 */
                override fun onSeekTo(pos: Long) {
                    super.onSeekTo(pos)
                    exoPlayer.seekTo(pos)
                }

                /** 止めた時 */
                override fun onStop() {
                    super.onStop()
                    isActive = false
                    stopSelf()
                }

                override fun onSkipToPrevious() {
                    super.onSkipToPrevious()
                    exoPlayer.previous()
                }

                override fun onSkipToNext() {
                    super.onSkipToNext()
                    exoPlayer.next()
                }

            })
            // 忘れずに
            setSessionToken(sessionToken)
        }

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
        })

    }

    /**
     * 再生状態とメタデータを設定する。今回はメタデータはハードコートする
     *
     * MediaSessionのsetCallBackで扱う操作([MediaSessionCompat.Callback.onPlay]など)も[PlaybackStateCompat.Builder.setState]に書かないと何も起きない
     * */
    private fun updateState() {
        val stateBuilder = PlaybackStateCompat.Builder().apply {
            // 取り扱う操作。とりあえず 再生準備 再生 一時停止 シーク を扱うようにする。書き忘れると何も起きない
            setActions(PlaybackStateCompat.ACTION_PREPARE or PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_SEEK_TO or PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
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
            putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, MediaAccess.getThumb(this@VideoMediaBrowserService, video.id))
            putLong(MediaMetadataCompat.METADATA_KEY_DURATION, exoPlayer.duration) // これあるとAndroid 10でシーク使えます
        }.build()
        mediaSessionCompat.setMetadata(mediaMetadataCompat)
        // 最後に聞いた曲を保存
        prefSettings.edit {
            putString("last_play_title", video.title)
            putLong("last_play_id", video.id)
            putString("last_play_artist", video.title)
        }
    }

    /** 通知を表示する */
    private fun showNotification() {
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
        }
    }

    /**
     * [onLoadChildren]で返すアイテムを作成する
     * */
    private fun createMediaItem(videoId: String, title: String, subTitle: String): MediaBrowserCompat.MediaItem {
        val mediaDescriptionCompat = MediaDescriptionCompat.Builder().apply {
            setTitle(title)
            setSubtitle(subTitle)
            setIconBitmap(MediaAccess.getThumb(this@VideoMediaBrowserService, videoId.toLong()))
            setMediaId(videoId)
        }.build()
        return MediaBrowserCompat.MediaItem(mediaDescriptionCompat, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSessionCompat.release()
        exoPlayer.release()
    }


}