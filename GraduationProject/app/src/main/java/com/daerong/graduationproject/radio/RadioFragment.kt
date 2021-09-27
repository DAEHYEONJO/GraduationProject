package com.daerong.graduationproject.radio

import android.annotation.SuppressLint
import android.content.ContextWrapper
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import com.daerong.graduationproject.R
import com.daerong.graduationproject.databinding.FragmentRadioBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.text.SimpleDateFormat

class RadioFragment : Fragment() {
    var binding : FragmentRadioBinding? = null
    var mediaRecord : MediaRecorder? = null
    var mediaPlayer : MediaPlayer?=null
    var curFile : File? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentRadioBinding.inflate(layoutInflater,container,false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBtn()
    }

    private fun initRecoder(){
        try {
            mediaRecord = MediaRecorder()
            mediaRecord?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(getRecordingFilePath())
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                prepare()
                start()
            }
        }catch (e : Exception){
            e.printStackTrace()
        }
    }

    private fun releasRecoder(){
        mediaRecord?.apply {
            stop()
            release()
        }
        mediaRecord = null
    }

    private fun playRecorded(){
        Firebase.firestore.collection("Radio").document("hi").get().addOnSuccessListener {
            val byteArrayInputStream : ByteArrayInputStream = it.get("path").toString().byteInputStream()
            val file = File(ContextWrapper(requireContext()).getExternalFilesDir(Environment.DIRECTORY_MUSIC),"new.mp3")
            val fileOutputStream = FileOutputStream(file)
            val bytes = byteArrayOf()
            var read = 0
            do {
                read = byteArrayInputStream.read(bytes)
                fileOutputStream.write(bytes,0,read)
            }while (read!=-1)
            try {
                mediaPlayer = MediaPlayer()
                mediaPlayer?.run {
                    this.setDataSource(file.path)
                    this.prepare()
                    this.start()
                }
            }catch (e : Exception){
                e.printStackTrace()
            }
        }

    }

    @SuppressLint("SimpleDateFormat")
    private fun getRecordingFilePath():String{
        val simpleDateFormat = SimpleDateFormat("yyyy_MM_dd_hh:mm:ss")
        val now = System.currentTimeMillis()
        val date = simpleDateFormat.format(now)
        val contextWrapper = ContextWrapper(requireContext())
        val musicDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val file = File(musicDirectory, "$date.mp3")
        curFile = file
        return file.path
    }

    private fun initBtn() {
        binding!!.run {
            recordBtn.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked){
                    //record start
                    Toast.makeText(requireContext(), "녹음시작", Toast.LENGTH_SHORT).show()
                    initRecoder()
                    val curFileUri = curFile?.toURI()

                    if (curFileUri != null) {
                        Firebase.firestore.collection("Radio").document("hi").set(hashMapOf("ho" to curFile?.inputStream()?.readBytes().toString()))
                    }
                }else{
                    //record release
                    Toast.makeText(requireContext(), "녹음중지", Toast.LENGTH_SHORT).show()
                    releasRecoder()
                }
            }
            playBtn.setOnClickListener {
                playRecorded()
            }
        }
    }

}