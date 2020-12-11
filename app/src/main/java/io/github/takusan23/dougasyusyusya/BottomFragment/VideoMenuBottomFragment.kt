package io.github.takusan23.dougasyusyusya.BottomFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.dougasyusyusya.DataClass.VideoDataClass
import io.github.takusan23.dougasyusyusya.ViewModel.FileListFragment
import io.github.takusan23.dougasyusyusya.ViewModel.FileListFragmentViewModel
import io.github.takusan23.dougasyusyusya.databinding.BottomFragmentVideoMenuBinding

/**
 * めにゅー
 * */
class VideoMenuBottomFragment : BottomSheetDialogFragment() {

    private lateinit var bottomFragmentVideoMenuBinding: BottomFragmentVideoMenuBinding

    /** ViewModel */
    private val viewModel by viewModels<FileListFragmentViewModel>({ requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bottomFragmentVideoMenuBinding = BottomFragmentVideoMenuBinding.inflate(inflater)
        return bottomFragmentVideoMenuBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 動画情報
        val videoData = requireArguments().getSerializable("data") as VideoDataClass

        bottomFragmentVideoMenuBinding.bottomFragmentVideoMenuPlayMusicTextview.setOnClickListener {
            // こっから再生
        }

        bottomFragmentVideoMenuBinding.bottomFragmentVideoMenuAudioConvertTextview.setOnClickListener {
            // 音楽変換
        }

        bottomFragmentVideoMenuBinding.bottomFragmentVideoMenuDeleteTextview.setOnClickListener {
            // 削除ボタン
            viewModel.deleteVideo(videoData)
        }

    }

}