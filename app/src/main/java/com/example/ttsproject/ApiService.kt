package com.example.ttsproject

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @Multipart
    @POST("/transcribe/")
    fun transcribeAudio(
        @Part file: MultipartBody.Part,
        @Part("target_lang") targetLang: RequestBody
    ): Call<TranscriptionResponse>
}