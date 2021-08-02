package com.daerong.graduationproject.insertcar

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.daerong.graduationproject.databinding.FragmentInsertCarInfoBinding
import com.daerong.graduationproject.viewmodel.InsertCarViewModel


class InsertCarInfoFragment : Fragment() {

    private var binding : FragmentInsertCarInfoBinding? = null
    private val insertCarViewModel : InsertCarViewModel by activityViewModels<InsertCarViewModel>()

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
    }

    private fun initText() {
        insertCarViewModel.run {
            curParkingLotName.observe(viewLifecycleOwner, Observer {
                binding!!.parkingLotName.text = "$it 주차장"
            })
            curParkingLotSection.observe(viewLifecycleOwner, Observer {
                binding!!.parkingLotSection.text = "${it}구역"
            })
            curCarNum.observe(viewLifecycleOwner, Observer {
                binding!!.carNum.text = it
            })
            carPhotoUri.observe(viewLifecycleOwner, Observer {
            })
        }
    }


    override fun onDestroy() {
        super.onDestroy()
    }
}