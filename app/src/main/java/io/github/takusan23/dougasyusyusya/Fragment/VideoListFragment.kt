package io.github.takusan23.dougasyusyusya.ViewModel

import android.content.ComponentName
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.github.takusan23.dougasyusyusya.Adapter.VideoListAdapter
import io.github.takusan23.dougasyusyusya.Fragment.MusicFragment
import io.github.takusan23.dougasyusyusya.R
import io.github.takusan23.dougasyusyusya.Service.VideoMediaBrowserService
import io.github.takusan23.dougasyusyusya.Tool.MotionLayoutTool
import io.github.takusan23.dougasyusyusya.databinding.FragmentFileListBinding
import kotlinx.coroutines.*

/**
 * ダウンロード一覧Fragment
 * */
class VideoListFragment : Fragment() {

    /** ViewModel */
    private val viewModel by viewModels<ViewoListFragmentViewModel>()

    lateinit var adapter: VideoListAdapter

    /** ViewBinding */
    private lateinit var viewBinding: FragmentFileListBinding

    // MediaBrowserServiceへ接続するなど
    var mediaBrowserCompat: MediaBrowserCompat? = null
    var mediaControllerCompat: MediaControllerCompat? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewBinding = FragmentFileListBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView初期化
        initRecyclerView()

        // MediaSession初期化
        initMediaSession()

        setMusicUIVisibility(false)

        // 音楽再生
        viewModel.selectVideo.observe(viewLifecycleOwner) { data ->
            mediaControllerCompat?.transportControls?.playFromMediaId(data.id.toString(), null)
        }

        // 一覧表示
        viewModel.videoList.observe(viewLifecycleOwner) { videoList ->
            adapter.notifyDataSetChanged()
        }

        // くるくる
        viewModel.loadingVideoList.observe(viewLifecycleOwner) { isLoading ->
            viewBinding.fragmentFileListSwipeRefresh.isRefreshing = isLoading
        }

        // 音楽再生
        viewBinding.fragmentFileListFab.setOnClickListener {
            if (MediaControllerCompat.getMediaController(requireActivity()).playbackState.state == PlaybackStateCompat.STATE_PAUSED) {
                // 再生へ
                mediaControllerCompat?.transportControls?.play()
                setMusicUIVisibility(true)
            } else {
                // 終了へ
                mediaControllerCompat?.transportControls?.pause()
                setMusicUIVisibility(false)
            }
        }

        // 一覧更新
        viewBinding.fragmentFileListSwipeRefresh.setOnRefreshListener {
            viewModel.loadVideoList()
        }

    }

    /** [io.github.takusan23.dougasyusyusya.Fragment.MusicFragment]を置く関数 */
    private fun setMusicFragment() {
        childFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_video_list_music_list_framelayout, MusicFragment())
            commit()
        }
    }

    /** 音楽UIを表示させるかどうか */
    private fun setMusicUIVisibility(isVisible: Boolean) {
        if (isVisible) {
            viewBinding.fragmentFileListFab.show()
            viewBinding.fragmentFileListFab.text = "音楽モード終了"
            viewBinding.fragmentVideoListMusicListBackground.visibility = View.VISIBLE
        } else {
            // BottomSheet閉じる
            viewBinding.fragmentFileListFab.text = "音楽モード"
            BottomSheetBehavior.from(viewBinding.fragmentVideoListMusicListBackground).state = BottomSheetBehavior.STATE_COLLAPSED
            viewBinding.fragmentVideoListMusicListBackground.visibility = View.INVISIBLE
        }
    }

    /** [VideoMediaBrowserService]と接続する */
    private fun initMediaSession() {
        mediaBrowserCompat = MediaBrowserCompat(requireContext(), ComponentName(requireContext(), VideoMediaBrowserService::class.java), object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                super.onConnected()
                if (mediaBrowserCompat != null && isAdded) {

                    // ActivityとMediaBrowserServiceを連携
                    mediaControllerCompat = MediaControllerCompat(requireContext(), mediaBrowserCompat!!.sessionToken)
                    MediaControllerCompat.setMediaController(requireActivity(), mediaControllerCompat)

                    // 音楽Fragment設置
                    setMusicFragment()

                    // 音楽操作
                    val mediaController = MediaControllerCompat.getMediaController(requireActivity())
                    mediaController?.registerCallback(object : MediaControllerCompat.Callback() {
                        /** 音楽変わったら */
                        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                            super.onMetadataChanged(metadata)
                            viewBinding.fragmentVideoListMusicListTitleTextview.text = metadata?.getText(MediaMetadataCompat.METADATA_KEY_TITLE)
                            viewBinding.fragmentVideoListMusicListImageview.setImageBitmap(metadata?.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART))
                        }

                        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                            super.onPlaybackStateChanged(state)
                            // 再生中ならUI表示
                            if (mediaController.playbackState?.state == PlaybackStateCompat.STATE_PLAYING) {
                                setMusicUIVisibility(true)
                            }
                        }

                    })

                    if (mediaController?.playbackState?.state == null) {
                        // 初回時
                        // とりあえずprepareよぶ
                        mediaControllerCompat?.transportControls?.prepare()
                    } else {
                        // すでに再生中
                        if (mediaController.playbackState?.state == PlaybackStateCompat.STATE_PLAYING) {
                            setMusicUIVisibility(true)
                        }
                        viewBinding.fragmentVideoListMusicListTitleTextview.text = mediaController.metadata.getText(MediaMetadataCompat.METADATA_KEY_TITLE)
                        viewBinding.fragmentVideoListMusicListImageview.setImageBitmap(mediaController.metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART))
                    }

                }
            }
        }, null)

        mediaBrowserCompat?.connect()
    }

    /** あとしまつ */
    override fun onDestroy() {
        super.onDestroy()
        // 切断
        mediaBrowserCompat?.disconnect()
        // 音楽UIない場合はService終了
        if (viewBinding.fragmentVideoListMusicListBackground.visibility == View.INVISIBLE) {
            mediaControllerCompat?.transportControls?.stop()
        }
    }

    /** RecyclerView用意 */
    private fun initRecyclerView() {
        adapter = VideoListAdapter(viewModel, childFragmentManager)
        viewBinding.fragmentFileListRecyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            addItemDecoration(itemDecoration)
            adapter = this@VideoListFragment.adapter
        }
    }

}