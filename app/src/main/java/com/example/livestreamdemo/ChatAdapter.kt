package com.example.livestreamdemo

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.bumptech.glide.Glide


class ChatAdapter(private var mList: List<LiveChatMessage>, val context: Context) :
    RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image)
        val textView: TextView = itemView.findViewById(R.id.tv_chat)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_layout, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemsViewModel = mList[position]

        Glide.with(context).load(itemsViewModel.authorDetails.profileImageUrl).centerCrop()
            .placeholder(R.drawable.round_logo).circleCrop().into(holder.imageView)
        Log.e("TAG", "onBindViewHolder: ${itemsViewModel.authorDetails.profileImageUrl}")

        holder.textView.text = itemsViewModel.snippet.textMessageDetails.messageText
    }

    fun updateList(mList: List<LiveChatMessage>) {
        this.mList = mList
        notifyDataSetChanged()
    }

}