package com.daerong.graduationproject

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.daerong.graduationproject.databinding.BeaconRowBinding
import com.minew.beaconset.MinewBeacon

class BeaconAdapter(var list : ArrayList<BeaconData>):RecyclerView.Adapter<BeaconAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: BeaconRowBinding):RecyclerView.ViewHolder(binding.root){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = BeaconRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.minor.text = list[position].minor
        holder.binding.rssi.text = list[position].rssi.toString()
        holder.binding.distance.text = list[position].dist.toString()
        //holder.binding.ratio.text = list[position].ratio.toString()
    }
}