package io.github.takusan23.dougasyusyusya.Tool

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.system.Os.close
import android.util.Size
import io.github.takusan23.dougasyusyusya.DataClass.VideoDataClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MediaStoreとかややこしいんだよ
 *
 * MediaStore関係。長くなるので切り出した。
 * */
object MediaStoreTool {

    /**
     * 動画一覧を取得する。コルーチンになりました。
     * @param context Context
     * */
    suspend fun getVideoList(context: Context): ArrayList<VideoDataClass> = withContext(Dispatchers.IO) {
        val list = arrayListOf<VideoDataClass>()
        // 動画を取り出す
        val query = if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                null,
                "relative_path = ?",
                arrayOf("${Environment.DIRECTORY_MOVIES}/DougaSyuSyuSya/"), // READ_EXTERNAL_STORAGE権限なければこれもいらない(自分のファイルしか見れないため)
                null,
            )
        } else {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                null,
                "_data like ?",
                arrayOf("%DougaSyuSyuSya%"), // READ_EXTERNAL_STORAGE権限なければこれもいらない(自分のファイルしか見れないため)
                null,
            )
        }
        query?.apply {
            // 取り出す。なんかforよりwhileのほうが良さそう(Androidのサイトに乗ってた)
            while (moveToNext()) {
                // idを取ってUriを取る
                val id = getLong(getColumnIndex(MediaStore.Video.Media._ID))
                val title = getString(getColumnIndex(MediaStore.Video.Media.TITLE))
                val fileName = getString(getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME))
                val extension = getString(getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)).split(".").last()
                // uri取得
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val thumb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(uri, Size(640, 480), null)
                } else {
                    // 動作未検証
                    MediaStore.Video.Thumbnails.getThumbnail(context.contentResolver, id, MediaStore.Video.Thumbnails.MICRO_KIND, BitmapFactory.Options())
                }
                list.add(VideoDataClass(title, fileName, id, uri, thumb, extension))
            }
            close()
        }
        return@withContext list
    }

    /**
     * 指定したIdの動画を取得する
     * */
    fun getVideo(context: Context, mediaId: Long): VideoDataClass {
        val list = arrayListOf<VideoDataClass>()
        // 動画を取り出す
        val query = if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                null,
                "_id = ?",
                arrayOf(mediaId.toString()), // READ_EXTERNAL_STORAGE権限なければこれもいらない(自分のファイルしか見れないため)
                null
            )
        } else {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                null,
                "_id = ?",
                arrayOf(mediaId.toString()),
                null
            )
        }
        query?.apply {
            // 取り出す。なんかforよりwhileのほうが良さそう(Androidのサイトに乗ってた)
            while (moveToNext()) {
                // idを取ってUriを取る
                val id = getLong(getColumnIndex(MediaStore.Video.Media._ID))
                val title = getString(getColumnIndex(MediaStore.Video.Media.TITLE))
                val fileName = getString(getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME))
                val extension = getString(getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)).split(".").last()
                // uri取得
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val thumb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(uri, Size(640, 480), null)
                } else {
                    // 動作未検証
                    MediaStore.Video.Thumbnails.getThumbnail(context.contentResolver, id, MediaStore.Video.Thumbnails.MICRO_KIND, BitmapFactory.Options())
                }
                list.add(VideoDataClass(title, fileName, id, uri, thumb, extension))
            }
            close()
        }
        return list[0]
    }

    /**
     * Uriからサムネを取得する
     * */
    fun getThumb(context: Context, id: Long): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id), Size(640, 480), null)
        } else {
            // 動作未検証
            MediaStore.Video.Thumbnails.getThumbnail(context.contentResolver, id, MediaStore.Video.Thumbnails.MICRO_KIND, BitmapFactory.Options())
        }

    }

    /**
     * 動画を消す
     * @param context その名の通り
     * @param uri Uri
     * @param id Uriだけじゃ削除できねえのかよ。まじでMediaStoreつかえな
     * */
    fun deleteMedia(context: Context, uri: Uri, id: Long) {
        context.contentResolver.delete(uri, "_id = ?", arrayOf(id.toString()))
    }

}