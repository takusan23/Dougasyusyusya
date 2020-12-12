package io.github.takusan23.dougasyusyusya.Fragment

import android.app.PendingIntent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Message
import android.support.v4.media.session.IMediaControllerCallback
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.Player
import io.github.takusan23.dougasyusyusya.Adapter.MusicListAdapter
import io.github.takusan23.dougasyusyusya.Adapter.VideoListAdapter
import io.github.takusan23.dougasyusyusya.R
import io.github.takusan23.dougasyusyusya.ViewModel.ViewoListFragmentViewModel
import io.github.takusan23.dougasyusyusya.databinding.FragmentMusicBinding

/**
 * 音楽操作用Fragment
 *
 * したから引っ張り出して表示するやつ
 * */
class MusicFragment : Fragment() {

    private lateinit var musicBinding: FragmentMusicBinding

    /** [io.github.takusan23.dougasyusyusya.ViewModel.VideoListFragment]のViewModelと共有 */
    private val viewModel by viewModels<ViewoListFragmentViewModel>({ requireParentFragment() })

    /** 音楽操作で使う */
    private var mediaControllerCompat: MediaControllerCompat? = null

    /** RecyclerViewのAdapter */
    lateinit var adapter: MusicListAdapter

    /** 曲の情報変わったときに受け取るコールバック。*/
    private val callback = object : MediaControllerCompat.Callback() {

        /** 再生、一時停止変更リスナー */
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)

            val drawable = when (state?.state) {
                PlaybackStateCompat.STATE_PLAYING -> requireContext().getDrawable(R.drawable.ic_outline_pause_24)
                else -> requireContext().getDrawable(R.drawable.ic_outline_play_arrow_24)
            }

            musicBinding.fragmentMusicPlay.setImageDrawable(drawable)
        }

        override fun onShuffleModeChanged(shuffleMode: Int) {
            super.onShuffleModeChanged(shuffleMode)
            musicBinding.fragmentMusicShuffle.imageTintList = when (shuffleMode) {
                PlaybackStateCompat.SHUFFLE_MODE_ALL -> ColorStateList.valueOf(Color.parseColor("#1976d2"))
                else -> ColorStateList.valueOf(Color.parseColor("#000000"))
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            super.onRepeatModeChanged(repeatMode)
            // アイコン直す
            val repeatIcon = when (repeatMode) {
                PlaybackStateCompat.REPEAT_MODE_ONE -> requireContext().getDrawable(R.drawable.ic_baseline_repeat_one_24)
                else -> requireContext().getDrawable(R.drawable.ic_baseline_repeat_24)
            }
            musicBinding.fragmentMusicRepeat.setImageDrawable(repeatIcon)
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            super.onQueueChanged(queue)
            // 曲一覧
            adapter.musicList.apply {
                clear()
                queue?.forEach { item -> add(item.description) }
            }
            adapter.notifyDataSetChanged()
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        musicBinding = FragmentMusicBinding.inflate(inflater)
        return musicBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /** [io.github.takusan23.dougasyusyusya.ViewModel.VideoListFragment]のMediaControlを取得 */
        mediaControllerCompat = MediaControllerCompat.getMediaController(requireActivity())

        // 再生ボタン
        musicBinding.fragmentMusicPlay.setOnClickListener {
            // さいせい、一時停止
            val state = mediaControllerCompat?.playbackState?.state
            if (state == PlaybackStateCompat.STATE_PLAYING) {
                mediaControllerCompat?.transportControls?.pause()
            } else {
                mediaControllerCompat?.transportControls?.play()
            }
        }

        // つぎのきょくとか
        musicBinding.fragmentMusicPrev.setOnClickListener {
            mediaControllerCompat?.transportControls?.skipToPrevious()
        }
        musicBinding.fragmentMusicNext.setOnClickListener {
            mediaControllerCompat?.transportControls?.skipToNext()
        }

        // シャッフルモードとか
        musicBinding.fragmentMusicShuffle.setOnClickListener {
            // 操作
            when (mediaControllerCompat?.shuffleMode) {
                PlaybackStateCompat.SHUFFLE_MODE_ALL -> mediaControllerCompat?.transportControls?.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)
                else -> mediaControllerCompat?.transportControls?.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL)
            }
        }

        // リピートボタン
        musicBinding.fragmentMusicRepeat.setOnClickListener {
            // 操作
            when (mediaControllerCompat?.repeatMode) {
                PlaybackStateCompat.REPEAT_MODE_ALL -> mediaControllerCompat?.transportControls?.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE)
                else -> mediaControllerCompat?.transportControls?.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
            }
        }

        // RecyclerView初期化
        initRecyclerView()

    }

    override fun onStart() {
        super.onStart()
        // コールバック。なんでこのタイミングで登録しているかと言うとisAddedがfalseの可能性があるから
        mediaControllerCompat?.registerCallback(callback)
    }

    override fun onStop() {
        super.onStop()
        mediaControllerCompat?.unregisterCallback(callback)
    }

    /** RecyclerView用意 */
    private fun initRecyclerView() {
        adapter = MusicListAdapter(arrayListOf())
        musicBinding.fragmentMusicRecyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            addItemDecoration(itemDecoration)
            adapter = this@MusicFragment.adapter
        }
    }

}