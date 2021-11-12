package com.daerong.graduationproject.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.daerong.graduationproject.data.InsertCar
import com.daerong.graduationproject.databinding.CarListRowBinding

class CarListAdapter(val items:ArrayList<InsertCar>): RecyclerView.Adapter<CarListAdapter.MyViewHolder>() {
    interface OnBtnClickListener{
        fun OnBtnClick(holder: RecyclerView.ViewHolder, view: View, data: InsertCar, position:Int)
    }

    var btnClickListener: OnBtnClickListener?=null

    inner class MyViewHolder(val binding: CarListRowBinding): RecyclerView.ViewHolder(binding.root){
        init{
            binding.exitAcceptBtn.setOnClickListener {
                btnClickListener?.OnBtnClick(this, it, items[adapterPosition], adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = CarListRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.binding.apply {
            licenseView.text = items[position].carNum
            startLocView.text = "${items[position].parkingLotName} ${items[position].parkingSection}구역"
            arrivalLocView.text = "요거프레소"

            if(items[position].carStatus == 1){
                exitAcceptBtn.visibility = View.VISIBLE
                accentView.visibility = View.VISIBLE
            }
            else{
                exitAcceptBtn.visibility = View.GONE
                accentView.visibility = View.INVISIBLE
            }
        }

    }

    override fun getItemCount(): Int {
        return items.size
    }
}