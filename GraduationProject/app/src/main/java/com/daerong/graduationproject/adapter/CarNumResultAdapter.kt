package com.daerong.graduationproject.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.daerong.graduationproject.databinding.CarNumResultRawBinding

class CarNumResultAdapter(var carNumList : ArrayList<String>) :
    RecyclerView.Adapter<CarNumResultAdapter.ViewHolder>() {

    inner class ViewHolder(val binding : CarNumResultRawBinding):RecyclerView.ViewHolder(binding.root){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = CarNumResultRawBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.carNumText.text = carNumList[position]
    }

    override fun getItemCount(): Int = carNumList.size
}