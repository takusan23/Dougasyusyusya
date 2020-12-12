package io.github.takusan23.dougasyusyusya.Adapter

import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaControllerCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.dougasyusyusya.R

/**
 * 曲一覧
 * */
class MusicListAdapter(val musicList: ArrayList<MediaDescriptionCompat>, val mediaControllerCompat: MediaControllerCompat?) : RecyclerView.Adapter<MusicListAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView = itemView.findViewById<TextView>(R.id.adapter_music_textview)
        val imageView = itemView.findViewById<ImageView>(R.id.adapter_music_imageview)
        val parent = itemView.findViewById<ConstraintLayout>(R.id.adapter_music_parent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.music_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            val context = textView.context
            val item = musicList[position]

            textView.text = item.title
            imageView.setImageBitmap(item.iconBitmap)

            parent.setOnClickListener {
                mediaControllerCompat?.transportControls?.playFromMediaId(item.mediaId, null)
            }

        }
    }

    override fun getItemCount(): Int {
        return musicList.size
    }
}