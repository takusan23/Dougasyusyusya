package io.github.takusan23.dougasyusyusya.Service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.os.*
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import io.github.takusan23.dougasyusyusya.R
import java.io.File
import java.nio.file.Files
import kotlin.concurrent.thread
import kotlin.math.roundToInt


/**
 * 動画DLするService。
 * ViewModelでも良かったかも？
 * */
class DownloadService : Service() {

    // 通知出すやつ
    private val notificationManager: NotificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    // 通知ID
    private val NOTIFICATION_ID = 4545

    // Broadcast。通知からサービスを止めるため
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // 無条件で終了で
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        // 通知を発行する
        createNotification("サービス準備中。少し待ってね")

        // broadcast
        registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction("download_service_stop")
        })

        // youtube-dl初期化
        try {
            YoutubeDL.getInstance().init(application)
        } catch (e: YoutubeDLException) {
            Toast.makeText(this, "youtube-dlの初期化に失敗しました", Toast.LENGTH_SHORT).show()
            stopSelf()
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // youtube-dlの更新かもしれない
        val isUpdate = intent?.getBooleanExtra("update", false) ?: false
        if (isUpdate) {
            createNotification("更新中")
            // インターネット接続なので別スレッドへ
            thread {
                // youtube-dl自体を更新。
                val status = YoutubeDL.getInstance().updateYoutubeDL(application)
                Handler(Looper.getMainLooper()).post {
                    // コールバック
                    when (status.ordinal) {
                        0 -> {
                            Toast.makeText(this, "更新できたよ", Toast.LENGTH_SHORT).show()
                        }
                        1 -> {
                            Toast.makeText(this, "更新済みです", Toast.LENGTH_SHORT).show()
                        }
                    }
                    stopSelf()
                }
            }
        } else {

            // URL受け取り
            val url = intent?.getStringExtra("url")
            if (url.isNullOrEmpty()) {
                Toast.makeText(this, "URLが見つかりませんでした", Toast.LENGTH_SHORT).show()
                stopSelf()
                return START_NOT_STICKY
            }

            // 保存先、URL設定
            // 保存先フォルダ。基本空にしといて、MediaStoreへコピーする際は0番目のファイルを指定するようにする。
            val youtubeDLDir = File(getExternalFilesDir(null), "youtube-dl_temp")
            youtubeDLDir.listFiles { file -> file.delete() } // からっぽのまにまに
            val request = YoutubeDLRequest(url)
            request.addOption("-o", youtubeDLDir.absolutePath + "/%(title)s.%(ext)s");
            // 多分同期処理なので別スレッド立ち上げ
            thread {

                // 保存開始
                try {
                    // タイトル取得
                    val videoInfo = YoutubeDL.getInstance().getInfo(url)

                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "ダウンロード開始します", Toast.LENGTH_SHORT).show()
                    }

                    YoutubeDL.getInstance().execute(request) { progress: Float, seconds: Long ->

                        createNotification(videoInfo.title, progress.roundToInt(), 100)
                        if (progress >= 100f) {
                            // MediaStoreを利用してVideoフォルダにお引越し
                            moveMediaStore(videoInfo, youtubeDLDir.listFiles()[0])
                            // ダウンロードしたファイルはMediaStoreに引っ越したので消す
                            youtubeDLDir.listFiles { file -> file.delete() } // からっぽのまにまに
                            Handler(Looper.getMainLooper()).post {
                                // 通知終了
                                Toast.makeText(this, "ダウンロードできたよ！\n「${videoInfo.title}」", Toast.LENGTH_SHORT).show()
                                stopSelf()
                            }
                        }
                    }

                } catch (e: YoutubeDLException) {
                    e.printStackTrace()
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "失敗しました。「動かないときに押すボタン」を押してみてください。：$e", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }
        return START_NOT_STICKY
    }

    /**
     * MediaStoreへダウンロードしたファイルを移動させる。
     *
     * MediaStoreを利用することで、Android Q以降もVideos / Audios / Picturesにアクセスできる
     * */
    private fun moveMediaStore(videoInfo: VideoInfo, file: File) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "${videoInfo.title}.${videoInfo.ext}")
            put(MediaStore.Video.Media.TITLE, videoInfo.title)
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            // 再生時間
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.DURATION, videoInfo.duration)
            }
        }
        if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
            // Android 10！！！。MediaStoreへ追加してUriをもらい書き込む
            // ディレクトリを作る。この保存先指定がAndroid 10以降にしかない
            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/DougaSyuSyuSya")
            val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return
            // MediaStore経由で保存する
            val outputStream = contentResolver.openOutputStream(uri, "w")
            // コピー
            outputStream?.write(file.inputStream().readBytes()) // Kotlinでらくする
            outputStream?.close()
        } else {
            // Android 9。この時代は直接JavaのFile APIが使えた。のでFileに書き込んでからMediaStoreへ挿入する
            val videoFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "DougaSyuSyuSya")
            if (!videoFolder.exists()) {
                videoFolder.mkdir() // なければ作成
            }
            // 書き込む
            val videoFile = File(videoFolder, "${videoInfo.title}.${videoInfo.ext}")
            videoFile.outputStream().write(file.inputStream().readBytes())
            // MediaStoreへ差し込む
            contentValues.put(MediaStore.Video.Media.DATA, videoFile.path)
            contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return
        }
    }


    /**
     * 通知を出す関数。Service開始から5秒以内に呼び出す必要がある
     * @param text 本文
     * @param max null以外でプログレスバーを出します。合計値
     * @param progress null以外でプログレスバーを出します。進捗
     * */
    private fun createNotification(text: String, progress: Int? = null, max: Int? = null) {
        val channelId = "download_notification"
        val notification = if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Oreo
            if (notificationManager.getNotificationChannel(channelId) == null) {
                // 通知チャンネル登録
                val notificationChannel = NotificationChannel(channelId, "ようつべダウンロード中通知", NotificationManager.IMPORTANCE_LOW)
                notificationManager.createNotificationChannel(notificationChannel)
            }
            NotificationCompat.Builder(this, channelId)
        } else {
            // Nougat
            NotificationCompat.Builder(this)
        }
        notification.apply {
            setContentTitle("動画収集車")
            setContentText(text)
            setSmallIcon(R.drawable.syu_syu_sya)
            // 終了ボタン
            val intent = Intent("download_service_stop")
            addAction(R.drawable.ic_outline_cloud_download_24, "終了", PendingIntent.getBroadcast(this@DownloadService, 114, intent, PendingIntent.FLAG_UPDATE_CURRENT))
            // 進捗
            if (progress != null && max != null) {
                setProgress(max, progress, false)
            }
        }
        startForeground(NOTIFICATION_ID, notification.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

}