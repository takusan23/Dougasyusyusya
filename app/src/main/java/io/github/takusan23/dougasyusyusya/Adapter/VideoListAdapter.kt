package io.github.takusan23.dougasyusyusya.Adapter

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.dougasyusyusya.BottomFragment.VideoMenuBottomFragment
import io.github.takusan23.dougasyusyusya.R
import io.github.takusan23.dougasyusyusya.ViewModel.FileListFragmentViewModel

/**
 * 動画一覧Fragmentで使うやつ
 * */
class VideoListAdapter(private val viewModel: FileListFragmentViewModel,val parentFragmentManager:FragmentManager) : RecyclerView.Adapter<VideoListAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val parent = itemView.findViewById<ConstraintLayout>(R.id.adapter_parent)
        val imageView = itemView.findViewById<ImageView>(R.id.adapter_imageview)
        val textView = itemView.findViewById<TextView>(R.id.adapter_textview)
        val menuButton = itemView.findViewById<ImageView>(R.id.adapter_menu_imageview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_video_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            val videoDataList = viewModel.videoList.value ?: return
            val videoData = videoDataList[position]
            val context = textView.context

            textView.text = videoData.title
            imageView.setImageBitmap(videoData.thumb)
            // 押したら動画再生アプリ起動
            parent.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, videoData.uri)
                context.startActivity(intent)
            }

            // めにゅー
            menuButton.setOnClickListener {
               val menuBottomFragment = VideoMenuBottomFragment().apply {
                   arguments = Bundle().apply {
                       putSerializable("data",videoData)
                   }
               }
                menuBottomFragment.show(parentFragmentManager,"menu")
            }

        }
    }

    override fun getItemCount(): Int {
        return viewModel.videoList.value!!.size
    }
}