package com.daerong.graduationproject.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.daerong.graduationproject.data.CameraX
import com.daerong.graduationproject.databinding.CameraImageRawBinding

class CameraImgAdapter( var imgList : ArrayList<CameraX>, val context : Context) : RecyclerView.Adapter<CameraImgAdapter.ViewHolder>() {

    interface OnImageClickListener{
        fun onClick(cameraX: CameraX)
    }

    var listener : OnImageClickListener? = null

    inner class ViewHolder(val binding : CameraImageRawBinding):RecyclerView.ViewHolder(binding.root){
        init {
            binding.toggleBtn.setOnCheckedChangeListener { buttonView, isChecked ->
                Log.i("CameraImgAdapter","isChecked : ${isChecked}")
                imgList[adapterPosition].checked = isChecked
                Log.i("CameraImgAdapter","adapterPosition : ${adapterPosition} ischecked : ${imgList[adapterPosition].checked}")
            }
            binding.root.setOnClickListener {
                Log.i("CameraImgAdapter","adapterPosition : ${adapterPosition} root click ischecked before : ${imgList[adapterPosition].checked}")
                binding.toggleBtn.isChecked = !binding.toggleBtn.isChecked
                imgList[adapterPosition].checked = binding.toggleBtn.isChecked
                Log.i("CameraImgAdapter","adapterPosition : ${adapterPosition} root click ischecked after : ${imgList[adapterPosition].checked}")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CameraImageRawBinding.inflate(LayoutInflater.from(parent.context))
        Log.i("CameraImgAdapter","onCreateViewHolder " )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.binding.root).load(imgList[position].uri).centerCrop().into(holder.binding.captureImg)
        holder.binding.toggleBtn.isChecked = imgList[position].checked
        //holder.binding.captureImg.setImageURI(imgList[position].uri)
        Log.i("CameraImgAdapter","onBindViewHolder ~ ${holder.binding.toggleBtn.isChecked } / ${imgList[position].checked}" )
        Log.i("CameraImgAdapter","onBindViewHolder ~ ${imgList[position].uri.toString()}" )
    }

    override fun getItemCount(): Int = imgList.size
}