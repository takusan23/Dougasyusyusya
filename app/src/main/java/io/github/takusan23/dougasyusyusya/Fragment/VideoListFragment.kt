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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.dougasyusyusya.Adapter.VideoListAdapter
import io.github.takusan23.dougasyusyusya.Fragment.MusicFragment
import io.github.takusan23.dougasyusyusya.R
import io.github.takusan23.dougasyusyusya.Service.VideoMediaBrowserService
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
            mediaControllerCompat?.transportControls?.play()
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

    /** [VideoMediaBrowserService]と接続する */
    private fun initMediaSession() {
        mediaBrowserCompat = MediaBrowserCompat(requireContext(), ComponentName(requireContext(), VideoMediaBrowserService::class.java), object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                super.onConnected()
                if (mediaBrowserCompat != null && isAdded) {
                    mediaControllerCompat = MediaControllerCompat(requireContext(), mediaBrowserCompat!!.sessionToken)

                    // ActivityとMediaBrowserServiceを連携
                    MediaControllerCompat.setMediaController(requireActivity(), mediaControllerCompat)

                    // 音楽Fragment設置
                    setMusicFragment()

                    // とりあえずprepareを呼ぶ
                    val mediaController = MediaControllerCompat.getMediaController(requireActivity())
                    mediaController?.transportControls?.prepare()

                    mediaController?.registerCallback(object : MediaControllerCompat.Callback() {
                        /** 音楽変わったら */
                        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                            super.onMetadataChanged(metadata)
                            viewBinding.fragmentVideoListMusicListTitleTextview.text = metadata?.getText(MediaMetadataCompat.METADATA_KEY_TITLE)
                            viewBinding.fragmentVideoListMusicListImageview.setImageBitmap(metadata?.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART))
                        }
                    })

                }
            }
        }, null)

        mediaBrowserCompat?.connect()
    }

    override fun onStop() {
        super.onStop()
        // 接続
        mediaBrowserCompat?.disconnect()
    }

    /** あとしまつ */
    override fun onDestroy() {
        super.onDestroy()
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