package com.daerong.graduationproject.carlist

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daerong.graduationproject.adapter.CarListAdapter
import com.daerong.graduationproject.data.InsertCar
import com.daerong.graduationproject.databinding.FragmentCarListBinding
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CarListFragment : Fragment() {
    lateinit var binding: FragmentCarListBinding
    lateinit var adapter : CarListAdapter
    private val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCarListBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView()  {
        binding.recyclerView.layoutManager = LinearLayoutManager(context,
            LinearLayoutManager.VERTICAL, false)

        val dividerItemDecoration =
            DividerItemDecoration(binding.recyclerView.context, LinearLayoutManager(context).orientation)
        binding.recyclerView.addItemDecoration(dividerItemDecoration)

        var carList = ArrayList<InsertCar>()

        db.collection("CarList").addSnapshotListener { value, error ->
            if (error!=null) return@addSnapshotListener

            for (doc in value!!.documentChanges) {
                Log.d("listsnapshot", "for loop")
                val data = doc.document.data

                when (doc.type) {
                    DocumentChange.Type.ADDED -> {
                        Log.d("listsnapshot", "New Car: ${doc.document.data}")
                        // 입차 상태 or 출차 대기 상태로 들어오는 경우
                        val status = data["carStatus"].toString().toInt()
                        if(status == 2){
                            adapter.items.add(
                                InsertCar(
                                    parkingLotName = data["parkingLotName"].toString(),
                                    parkingSection = data["parkingSection"].toString(),
                                    carNum = data["carNum"].toString(),
                                    carStatus = data["carStatus"].toString().toInt(),
                                    approachStatus = data["approachStatus"].toString().toBoolean(),
                                    managed = data["managed"].toString()
                                )
                            )
                            adapter.notifyItemInserted(adapter.itemCount-1)
                        }
                        else if(status == 1){
                            adapter.items.add(0,
                                InsertCar(
                                    parkingLotName = data["parkingLotName"].toString(),
                                    parkingSection = data["parkingSection"].toString(),
                                    carNum = data["carNum"].toString(),
                                    carStatus = data["carStatus"].toString().toInt(),
                                    approachStatus = data["approachStatus"].toString().toBoolean(),
                                    managed = data["managed"].toString()
                                )
                            )
                            adapter.notifyItemInserted(0)
                        }
                    }
                    DocumentChange.Type.MODIFIED ->{
                        Log.d("listsnapshot", "Modified Car: ${doc.document.data}")
                        // 출차중 or 출차 수락으로 변경되는 경우
                        val car = InsertCar(
                            parkingLotName = data["parkingLotName"].toString(),
                            parkingSection = data["parkingSection"].toString(),
                            carNum = data["carNum"].toString(),
                            carStatus = data["carStatus"].toString().toInt(),
                            approachStatus = data["approachStatus"].toString().toBoolean(),
                            managed = data["managed"].toString()
                        )
                        if (data["carStatus"].toString().toInt() == 1){ // 출차 대기 상태
                            //리스트에서 삭제 후 맨 위로 올리기
                            car.carStatus = 2
                            val index = adapter.items.indexOf(car)
                            if(index!=-1){
                                adapter.items.removeAt(index)
                                adapter.notifyItemRemoved(index)
                            }
                            car.carStatus = 1
                            adapter.items.add(0, car)
                            adapter.notifyItemInserted(0)
                            binding.recyclerView.smoothScrollToPosition(0)
                            Log.d("listsnapshot", "출차 대기로 이동")
                        }
                        else if (data["carStatus"].toString().toInt() == 0){ // 출차 수락 상태
                            car.carStatus = 1
                            val index = adapter.items.indexOf(car)
                            if(index!=-1){
                                adapter.items.removeAt(index)
                                adapter.notifyItemRemoved(index)
                            }
                            Log.d("listsnapshot", "리스트에서 삭제")
                        }

                    }
                    DocumentChange.Type.REMOVED -> {
                        Log.d("listsnapshot", "Removed Car: ${doc.document.data}")
                        // 정상적이라면 이미 리스트에 없어야 함
                    }
                }
            }
        }
        adapter = CarListAdapter(carList)
        adapter.btnClickListener = object: CarListAdapter.OnBtnClickListener{
            override fun OnBtnClick(
                holder: RecyclerView.ViewHolder,
                view: View,
                data: InsertCar,
                position: Int
            ) {
                Log.d("listsnapshot", "Exit Btn Click")
                // DB수정, 혹시 미리 바껴 있으면 취소됨
                val docRef = db.collection("CarList").document(data.carNum)
                docRef.get()
                    .addOnSuccessListener {
                        if(it["carStatus"].toString().toInt() == 1){
                            docRef.update("carStatus", 0)
                            val bottomSheet = MyBottomSheetDialog(data)
                            bottomSheet.isCancelable = false
                            bottomSheet.show(childFragmentManager, bottomSheet.tag)
                        }
                        else{
                            Toast.makeText(context,
                                "이미 출차 중인 차량입니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        binding.recyclerView.adapter = adapter
    }


}