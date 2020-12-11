package io.github.takusan23.dougasyusyusya.ViewModel

import android.content.ComponentName
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.dougasyusyusya.Adapter.VideoListAdapter
import io.github.takusan23.dougasyusyusya.R
import io.github.takusan23.dougasyusyusya.Service.VideoMediaBrowserService
import io.github.takusan23.dougasyusyusya.databinding.FragmentFileListBinding

/**
 * ファイル一覧Fragment
 * */
class FileListFragment : Fragment() {

    /** ViewModel */
    private val viewModel by viewModels<FileListFragmentViewModel>()

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
        viewModel.selectVideo.observe()

        // 一覧表示
        viewModel.videoList.observe(viewLifecycleOwner) { videoList ->
            adapter.notifyDataSetChanged()
        }

        // 音楽再生
        viewBinding.fragmentFileListFab.setOnClickListener {
            mediaControllerCompat?.transportControls?.play()
        }

    }

    private fun initMediaSession() {
        mediaBrowserCompat = MediaBrowserCompat(requireContext(), ComponentName(requireContext(), VideoMediaBrowserService::class.java), object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                super.onConnected()
                if (mediaBrowserCompat != null) {
                    mediaControllerCompat = MediaControllerCompat(requireContext(), mediaBrowserCompat!!.sessionToken)
                    // とりあえずprepareを呼ぶ
                    mediaControllerCompat?.transportControls?.prepare()
                }
            }
        }, null)
        // 接続
        mediaBrowserCompat?.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaBrowserCompat?.disconnect()
    }

    private fun initRecyclerView() {
        adapter = VideoListAdapter(viewModel,parentFragmentManager)
        viewBinding.fragmentFileListRecyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            addItemDecoration(itemDecoration)
            adapter = this@FileListFragment.adapter
        }
    }

}