package com.example.ttsproject

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.example.ttsproject.databinding.ActivityMainBinding
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class MainActivity : ComponentActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var recordButton: Button
    private lateinit var languageSelector: Spinner
    private lateinit var sendButton: Button
    private lateinit var transcriptionText: TextView
    private lateinit var translationText: TextView

    private var recorder: MediaRecorder? = null
    private var audioFile: File? = null

    private val languagesMap = mapOf("Inglés" to "en",
                                     "Francés" to "fr")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recordButton = findViewById(R.id.recordButton)
        languageSelector = findViewById(R.id.languageSelector)
        sendButton = findViewById(R.id.sendButton)
        transcriptionText = findViewById(R.id.transcriptionText)
        translationText = findViewById(R.id.translationText)
        checkPermissions()

        transcriptionText.movementMethod = LinkMovementMethod.getInstance()
        translationText.movementMethod = LinkMovementMethod.getInstance()

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languagesMap.keys.toList()).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        languageSelector.adapter = adapter

        recordButton.setOnClickListener {
            if (recorder == null) {
                startRecording()
                recordButton.text = "Detener"
            } else {
                stopRecording()
                recordButton.text = "Grabar"
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
            Log.e("MediaRecorder", "La grabación falló", e)
        }

        Toast.makeText(this, "Grabando...", Toast.LENGTH_SHORT).show()
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        Toast.makeText(this, "Grabación guardada", Toast.LENGTH_SHORT).show()
    }

    private fun sendAudioToAPI(file: File){

        val selectedLanguage = languageSelector.selectedItem.toString()
        val languageCode = languagesMap[selectedLanguage] ?: "en"

        if(selectedLanguage.isEmpty()){
            Toast.makeText(this, "Por favor, selecciona un lenguaje de la lista: ", Toast.LENGTH_SHORT).show()
            return
        }

        val client = OkHttpClient()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://alkerg-stt-project.hf.space")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)

        val requestFile = file.asRequestBody("audio/wav".toMediaTypeOrNull())
        val audioBody = MultipartBody.Part.createFormData("file", file.name, requestFile)

        val languageBody = languageCode.toRequestBody("text/plain".toMediaTypeOrNull())

        val call = service.transcribeAudio(
            file = audioBody,
            targetLang = languageBody
        )

        call.enqueue(object : Callback<TranscriptionResponse> {
            override fun onResponse(
                call: Call<TranscriptionResponse>,
                response: Response<TranscriptionResponse>
            ) {
                if (response.isSuccessful) {
                    transcriptionText.text = response.body()?.transcription ?: "Transcripción no disponible"
                    translationText.text = response.body()?.translated ?: "Traducción no disponible"
                } else {
                    transcriptionText.text = "Error: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<TranscriptionResponse>, t: Throwable) {
                transcriptionText.text = "Failed: ${t.message}"
            }
        })
    }
}
