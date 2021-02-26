package com.lt.retrofit.sca.test.http

import com.lt.retrofit.sca.test.config.HttpConfig
import com.lt.retrofit.socketcalladapter.SocketCallAdapter
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * creator: lt  2020/9/23  lt.dygzs@qq.com
 * effect : 初始化和配置Retrofit
 * warning:
 */
object InitRetrofit {

    internal fun initHttpFunctions(): HttpFunctions {
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(30L, TimeUnit.SECONDS)
                .readTimeout(30L, TimeUnit.SECONDS)
                .writeTimeout(30L, TimeUnit.SECONDS)
                .connectionPool(ConnectionPool(5, 300, TimeUnit.SECONDS))

        val client = builder.build()

        val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(HttpConfig.ROOT_URL.toString())
                .addConverterFactory(GsonConverterFactory.create())
//                .client(client)
                .callFactory(SocketCallAdapter())
                .build()

        return retrofit.create(HttpFunctions::class.java)
    }
}