package com.example.relay

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// 앱이 실행될때 1번만 실행
class ApplicationClass : Application() {
    val API_URL = "http://3.38.32.124/"

    // 전역변수
    companion object {
        lateinit var prefs: SharedPreferences

        // JWT Token Header 키 값
        // val X_ACCESS_TOKEN = "-"

        // Retrofit 인스턴스, 앱 실행시 한번만 생성하여 사용
        lateinit var sRetrofit: Retrofit
    }

    // 앱이 처음 생성될 때
    override fun onCreate() {
        super.onCreate()

        // shared preference
        prefs =
            applicationContext.getSharedPreferences("prefs", MODE_PRIVATE)
        // retrofit instance
        initRetrofitInstance()
    }

    private fun initRetrofitInstance() {
        val client: OkHttpClient = OkHttpClient.Builder()
            .readTimeout(5000, TimeUnit.MILLISECONDS)
            .connectTimeout(5000, TimeUnit.MILLISECONDS)
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            // .addNetworkInterceptor(XAccessTokenInterceptor()) // JWT 자동 헤더 전송
            .build()


        sRetrofit = Retrofit.Builder()
            .baseUrl(API_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    }
}