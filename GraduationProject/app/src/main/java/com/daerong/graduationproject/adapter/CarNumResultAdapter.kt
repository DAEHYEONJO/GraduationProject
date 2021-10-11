package com.daerong.graduationproject.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.daerong.graduationproject.databinding.CarNumResultRawBinding

class CarNumResultAdapter(var carNumList : ArrayList<String>) :
    RecyclerView.Adapter<CarNumResultAdapter.ViewHolder>() {

    private var listener : OnItemClickListener? = null

    interface OnItemClickListener{
        fun onClick(carNum : String)
    }

    fun setOnItemClickListener(listener : OnItemClickListener){
        this.listener = listener
    }

    inner class ViewHolder(val binding : CarNumResultRawBinding):RecyclerView.ViewHolder(binding.root){
        init {
            binding.carNumText.setOnClickListener {
                listener?.onClick(carNumList[adapterPosition])
            }
        }
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