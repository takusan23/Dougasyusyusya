package io.github.takusan23.dougasyusyusya.ViewModel

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import io.github.takusan23.dougasyusyusya.databinding.FragmentDownloadBinding

/**
 * ダウンロード画面Fragment
 *
 * ダウンロード自体はServiceでやってる
 * */
class DownloadFragment : Fragment() {

    /** ViewModel */
    private val viewModel by viewModels<DownloadFragmentViewModel>()

    /** ViewBinding */
    private lateinit var fragmentDownloadBinding: FragmentDownloadBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentDownloadBinding = FragmentDownloadBinding.inflate(inflater)
        return fragmentDownloadBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentDownloadBinding.fragmentDownloadClipboardButton.setOnClickListener {
            pasteURL()
        }


        viewModel.currentDownloadURL.observe(viewLifecycleOwner) { url ->
            fragmentDownloadBinding.fragmentDownloadUrlEdittext.setText(url)
        }

        // もしURLがあれば
        viewModel.currentDownloadURL.value = arguments?.getString("url")

        // 取得ボタン
        fragmentDownloadBinding.fragmentDownloadButton.setOnClickListener {
            viewModel.download(fragmentDownloadBinding.fragmentDownloadUrlEdittext.text.toString())
        }

        // youtube-dl バイナリ更新ボタン
        fragmentDownloadBinding.fragmentDownloadUpdateButton.setOnClickListener {
            viewModel.updateYoutubeDl()
        }

    }

    /**
     * クリップボードからURLを見つける
     * */
    private fun pasteURL() {
        val clipboardManager = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboardManager.primaryClip?.getItemAt(0)?.text ?: return
        val url = regexUrl(text.toString())
        viewModel.currentDownloadURL.value = url
    }


}