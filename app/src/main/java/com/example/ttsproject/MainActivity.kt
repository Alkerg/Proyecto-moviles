package com.example.ttsproject

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.ttsproject.ui.theme.TTSProjectTheme
import com.example.ttsproject.databinding.ActivityMainBinding
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class MainActivity : ComponentActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var recordButton: Button
    private lateinit var sendButton: Button
    private lateinit var resultText: TextView

    private var recorder: MediaRecorder? = null
    private var audioFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recordButton = findViewById(R.id.recordButton)
        sendButton = findViewById(R.id.sendButton)
        resultText = findViewById(R.id.resultText)

        checkPermissions()

        recordButton.setOnClickListener {
            if (recorder == null) {
                startRecording()
                recordButton.text = "Stop"
            } else {
                stopRecording()
                recordButton.text = "Record"
            }
        }

        sendButton.setOnClickListener {
            audioFile?.let { sendAudioToAPI(it) }
        }

    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 200)
        }
    }

    private fun startRecording() {

        try{
            val output = File(getExternalFilesDir(null), "audio.wav")
            val path = output.absolutePath
            audioFile = output

            Log.d("AUDIO_PATH", "AUDIO FILE SABED IN: $path")

            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(output.absolutePath)
                prepare()
                start()
            }
        }catch(e: Exception){
            Log.e("MediaRecorder", "Recording failed", e)
        }

        Toast.makeText(this, "Recording...", Toast.LENGTH_SHORT).show()
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        Toast.makeText(this, "Saved recording", Toast.LENGTH_SHORT).show()
    }

    private fun sendAudioToAPI(file: File){
        val client = OkHttpClient()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://alkerg-stt-project.hf.space")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)

        val requestFile = file.asRequestBody("audio/wav".toMediaTypeOrNull())

        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        val call = service.transcribeAudio(
            file = body
        )

        call.enqueue(object : Callback<TranscriptionResponse> {
            override fun onResponse(
                call: Call<TranscriptionResponse>,
                response: Response<TranscriptionResponse>
            ) {
                if (response.isSuccessful) {
                    resultText.text = response.body()?.transcription ?: "Transcription not available"
                } else {
                    resultText.text = "Error: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<TranscriptionResponse>, t: Throwable) {
                resultText.text = "Failed: ${t.message}"
            }
        })
    }
}
