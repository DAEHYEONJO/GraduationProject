package com.daerong.graduationproject.insertcar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.daerong.graduationproject.databinding.FragmentInsertCarInfoBinding
import com.daerong.graduationproject.licenseplate.TestGetNumActivity
import com.daerong.graduationproject.viewmodel.InsertCarViewModel


class InsertCarInfoFragment : Fragment() {

    private var binding : FragmentInsertCarInfoBinding? = null
    private val insertCarViewModel : InsertCarViewModel by activityViewModels<InsertCarViewModel>()
    private val requestActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        //startActivityForResult 가 deprecated 되었음. 새로운 방법임.
        if (it.resultCode == 1012){
            Log.d("receivedata", it.data?.getStringExtra("carNumResult").toString())
            insertCarViewModel.curCarNum.value = it.data?.getStringExtra("carNumResult").toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentInsertCarInfoBinding.inflate(layoutInflater,container,false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initText()
        initBtn()
    }

    private fun initBtn() {
        binding?.run {
            carNumRecognizeLayout.setOnClickListener {
                val intent = Intent(requireContext(), TestGetNumActivity::class.java)
                requestActivity.launch(intent)
            }
        }
    }

    private fun initText() {
        insertCarViewModel.run {
            curParkingLotName.observe(viewLifecycleOwner, Observer {
                if (it!="") binding!!.parkingLotName.text = "$it 주차장"
                else binding!!.parkingLotName.text = "주차장 미선택"
            })
            curParkingLotSection.observe(viewLifecycleOwner, Observer {
                binding!!.parkingLotSection.text = "${it}구역"
            })
            curCarNum.observe(viewLifecycleOwner, Observer {
                binding!!.carNum.text = it
            })
            carPhotoFile.observe(viewLifecycleOwner, Observer {
                Glide.with(binding!!.root)
                        .load(it[0])
                        .override(500,200)
                        .apply (RequestOptions.bitmapTransform(CircleCrop()))
                        .into(binding!!.carImg)
            })
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}