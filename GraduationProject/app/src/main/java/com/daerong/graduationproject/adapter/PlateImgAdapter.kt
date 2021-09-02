package com.daerong.graduationproject.adapter

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.daerong.graduationproject.databinding.PlateImgRowBinding

class PlateImgAdapter(var list : ArrayList<Bitmap>) : RecyclerView.Adapter<PlateImgAdapter.ViewHolder>(){

    inner class ViewHolder(val binding : PlateImgRowBinding):RecyclerView.ViewHolder(binding.root){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(PlateImgRowBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.img.setImageBitmap(list[position])
    }

    override fun getItemCount(): Int = list.size
}