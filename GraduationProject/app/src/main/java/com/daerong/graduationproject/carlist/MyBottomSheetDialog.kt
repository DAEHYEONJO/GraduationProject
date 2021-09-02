package com.daerong.graduationproject.carlist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.daerong.graduationproject.R
import com.daerong.graduationproject.data.InsertCar
import com.daerong.graduationproject.databinding.BottomSheetLayoutBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MyBottomSheetDialog(private val carData: InsertCar) : BottomSheetDialogFragment() {
    lateinit var binding: BottomSheetLayoutBinding
    private val db = Firebase.firestore


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = BottomSheetLayoutBinding.inflate(layoutInflater, container, false)
        binding.carNumberView.text = carData.carNum
        //binding.destinationView.text = "신공학관"
        binding.parkingLotView.text = "${carData.parkingLotName} ${carData.parkingSection}구역"
        binding.exitCompleteBtn.setOnClickListener {
            Toast.makeText(context, "출차 완료 버튼 클릭", Toast.LENGTH_SHORT).show()
            val docRef = db.collection("CarList").document(carData.carNum)
            docRef.get().addOnSuccessListener {
                val parkingLot = it["parkingLotName"].toString()
                Log.d("deleteCar", "parkingLotName: $parkingLot")
                val parkRef = db.collection("ParkingLot").document(parkingLot)
                Log.d("deleteCar", "parkRef: $parkRef")
                parkRef.get().addOnSuccessListener { parking ->
                    Log.d("deleteCar", "parking: ${parking["curCarCount"]}")
                    val num = parking["curCarCount"].toString().toInt()
                    parkRef.update("curCarCount", num-1)
                }
                docRef.delete()
            }

            dismiss()
        }

        return binding.root
    }

}