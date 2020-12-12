package io.github.takusan23.dougasyusyusya.Adapter

import android.support.v4.media.MediaDescriptionCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.dougasyusyusya.R

/**
 * 曲一覧
 * */
class MusicListAdapter(val musicList: ArrayList<MediaDescriptionCompat>) : RecyclerView.Adapter<MusicListAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView = itemView.findViewById<TextView>(R.id.adapter_music_textview)
        val imageView = itemView.findViewById<ImageView>(R.id.adapter_menu_imageview)
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
            //imageView.setImageBitmap(item.iconBitmap)

        }
    }

    override fun getItemCount(): Int {
        return musicList.size
    }
}