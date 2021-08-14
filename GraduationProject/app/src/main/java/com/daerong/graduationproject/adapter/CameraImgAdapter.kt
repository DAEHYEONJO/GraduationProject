package com.daerong.graduationproject.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.daerong.graduationproject.data.CameraX
import com.daerong.graduationproject.databinding.ActivityCameraXBinding
import com.daerong.graduationproject.databinding.CameraImageRawBinding
import kotlinx.android.synthetic.main.activity_camera_x.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CameraImgAdapter( var imgList : ArrayList<CameraX>, val b : ActivityCameraXBinding) : RecyclerView.Adapter<CameraImgAdapter.ViewHolder>() {



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
        CoroutineScope(Dispatchers.Main).launch {

            Glide.with(holder.binding.root).load(imgList[position].file).centerCrop().skipMemoryCache(false).useAnimationPool(true).into(holder.binding.captureImg)
        }

        holder.binding.toggleBtn.isChecked = imgList[position].checked
        //holder.binding.captureImg.setImageURI(imgList[position].uri)
        Log.i("CameraImgAdapter","onBindViewHolder ~ ${holder.binding.toggleBtn.isChecked } / ${imgList[position].checked}" )
        Log.i("CameraImgAdapter","onBindViewHolder ~ ${imgList[position].file.toString()}" )
    }

    override fun getItemCount(): Int = imgList.size
}