package io.github.takusan23.dougasyusyusya.Fragment

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import android.util.TimeUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.dougasyusyusya.Adapter.MusicListAdapter
import io.github.takusan23.dougasyusyusya.R
import io.github.takusan23.dougasyusyusya.ViewModel.ViewoListFragmentViewModel
import io.github.takusan23.dougasyusyusya.databinding.FragmentMusicBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    /** RecyclerViewの項目を移動中の場合はtrue */
    private var isDraggingRecyclerViewItem = false

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

        /** シャッフルが変わると呼ばれる */
        override fun onShuffleModeChanged(shuffleMode: Int) {
            super.onShuffleModeChanged(shuffleMode)
            musicBinding.fragmentMusicShuffle.imageTintList = when (shuffleMode) {
                PlaybackStateCompat.SHUFFLE_MODE_ALL -> ColorStateList.valueOf(Color.parseColor("#1976d2"))
                else -> ColorStateList.valueOf(Color.parseColor("#000000"))
            }
        }

        /** リピートモードが変わると呼ばれる */
        override fun onRepeatModeChanged(repeatMode: Int) {
            super.onRepeatModeChanged(repeatMode)
            // アイコン直す
            val repeatIcon = when (repeatMode) {
                PlaybackStateCompat.REPEAT_MODE_ONE -> requireContext().getDrawable(R.drawable.ic_baseline_repeat_one_24)
                else -> requireContext().getDrawable(R.drawable.ic_baseline_repeat_24)
            }
            musicBinding.fragmentMusicRepeat.setImageDrawable(repeatIcon)
        }

        /**
         * 曲一覧更新があったら来る。シャッフルとか。
         * */
        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            super.onQueueChanged(queue)
            // 操作中は無視。並び替えのときも呼ばれてしまうので
            if (isDraggingRecyclerViewItem) return

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


        // しょきかー。すでにブラウザサービスが起動中だと来ないので取りに行く
        if (mediaControllerCompat?.playbackState != null) {
            // 再生ボタン
            val drawable = when (mediaControllerCompat?.playbackState?.state) {
                PlaybackStateCompat.STATE_PLAYING -> requireContext().getDrawable(R.drawable.ic_outline_pause_24)
                else -> requireContext().getDrawable(R.drawable.ic_outline_play_arrow_24)
            }
            musicBinding.fragmentMusicPlay.setImageDrawable(drawable)
            // シャッフル
            musicBinding.fragmentMusicShuffle.imageTintList = when (mediaControllerCompat?.shuffleMode) {
                PlaybackStateCompat.SHUFFLE_MODE_ALL -> ColorStateList.valueOf(Color.parseColor("#1976d2"))
                else -> ColorStateList.valueOf(Color.parseColor("#000000"))
            }
            // リピート
            val repeatIcon = when (mediaControllerCompat?.repeatMode) {
                PlaybackStateCompat.REPEAT_MODE_ONE -> requireContext().getDrawable(R.drawable.ic_baseline_repeat_one_24)
                else -> requireContext().getDrawable(R.drawable.ic_baseline_repeat_24)
            }
            musicBinding.fragmentMusicRepeat.setImageDrawable(repeatIcon)
        }

        /** 音楽の進捗 */
        initProgress()

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

    private fun initProgress() {
        /** SeekBar操作中ならtrue */
        var isTouchingSeekBar = false

        // プログレスバーは定期的に見に行くしか無い？
        lifecycleScope.launch {
            while (true) {
                delay(500)
                if(mediaControllerCompat?.metadata != null){
                    val duration = mediaControllerCompat?.metadata!!.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) / 1000L
                    val progress = mediaControllerCompat?.playbackState!!.position / 1000
                    // テキスト
                    musicBinding.apply {
                        fragmentMusicDuration.text = DateUtils.formatElapsedTime(duration)
                        fragmentMusicCurrent.text = DateUtils.formatElapsedTime(progress)
                        fragmentMusicSeekbar.max = duration.toInt()
                        // プログレスバー
                        if (!isTouchingSeekBar) {
                            fragmentMusicSeekbar.progress = progress.toInt()
                        }
                    }
                }
            }
        }
        // シークバー操作中はシークバー進めない のと ブラウザサービスを操作
        musicBinding.fragmentMusicSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaControllerCompat?.transportControls?.seekTo((progress * 1000).toLong())
                    isTouchingSeekBar = true
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isTouchingSeekBar = false
            }
        })
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
        // 曲一覧をブラウザサービスからもらう
        val queue = mediaControllerCompat?.queue
        adapter = MusicListAdapter(
            // MediaControllerが使えるならキューからもらう(メディアブラウザサービス起動中)。なければMediaBrowserからコールバックを待つので空の配列
            musicList = (queue?.map { queueItem -> queueItem.description } ?: arrayListOf()) as ArrayList<MediaDescriptionCompat>,
            mediaControllerCompat = mediaControllerCompat
        )
        // その他の設定
        musicBinding.fragmentMusicRecyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            addItemDecoration(itemDecoration)
            adapter = this@MusicFragment.adapter

            // ドラッグできるようにする
            val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT) {

                // 操作中
                override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                    // とりあえず見た目だけ(データの位置は変わってない)
                    adapter?.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
                    isDraggingRecyclerViewItem = true
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                }

                // 移動が一時完了
                override fun onMoved(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, fromPos: Int, target: RecyclerView.ViewHolder, toPos: Int, x: Int, y: Int) {
                    super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
                    // データを移動させる
                    mediaControllerCompat?.transportControls?.sendCustomAction("move", Bundle().apply {
                        putInt("from", fromPos)
                        putInt("to", toPos)
                    })
                }

                // 離したとき
                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)
                    isDraggingRecyclerViewItem = false
                }

            })
            itemTouchHelper.attachToRecyclerView(musicBinding.fragmentMusicRecyclerview)

        }
    }

}