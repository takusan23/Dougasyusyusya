package io.github.takusan23.dougasyusyusya.BottomFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.dougasyusyusya.ViewModel.VideoListFragment
import io.github.takusan23.dougasyusyusya.ViewModel.ViewoListFragmentViewModel
import io.github.takusan23.dougasyusyusya.databinding.BottomFragmentVideoMenuBinding

/**
 * めにゅー
 * */
class VideoMenuBottomFragment : BottomSheetDialogFragment() {

    private lateinit var bottomFragmentVideoMenuBinding: BottomFragmentVideoMenuBinding

    /** ViewModel。[VideoListFragment]のViewModelを利用している */
    private val viewModel by viewModels<ViewoListFragmentViewModel>({ requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bottomFragmentVideoMenuBinding = BottomFragmentVideoMenuBinding.inflate(inflater)
        return bottomFragmentVideoMenuBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 動画情報
        val videoData = viewModel.videoList.value?.get(requireArguments().getInt("pos"))
        if (videoData == null) {
            dismiss()
            return
        }

        // 終了LiveData
        viewModel.bottomFragmentCloseLiveData.observe(viewLifecycleOwner) {
            dismiss()
        }

        bottomFragmentVideoMenuBinding.bottomFragmentVideoMenuPlayMusicTextview.setOnClickListener {
            // こっから再生
            viewModel.startVideo(videoData)
        }

        bottomFragmentVideoMenuBinding.bottomFragmentVideoMenuAudioConvertTextview.setOnClickListener {
            // 音楽変換
            viewModel.videoToAudio(videoData)
        }

        bottomFragmentVideoMenuBinding.bottomFragmentVideoMenuDeleteTextview.setOnClickListener {
            // 削除ボタン
            viewModel.deleteVideo(videoData)
        }

    }

}