package com.lt.retrofit.sca.test.activity

import android.app.Activity
import android.os.Bundle
import com.lt.retrofit.sca.test.R
import com.lt.retrofit.sca.test.http.InitRetrofit
import com.lt.retrofit.sca.test.model.IpBean
import com.lt.retrofit.sca.test.model.NetBean
import com.lt.retrofit.sca.test.util.e
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val initHttpFunctions = InitRetrofit.initHttpFunctions()
//        val execute = initHttpFunctions.`user$login`("123", "345").execute().body()?.code
//        execute.e()
        initHttpFunctions.`user$login`("123", "345").enqueue(object : Callback<NetBean<IpBean>> {
            override fun onResponse(call: Call<NetBean<IpBean>>, response: Response<NetBean<IpBean>>) {
                response.body()?.code.e()
            }

            override fun onFailure(call: Call<NetBean<IpBean>>, t: Throwable) {
                t.e()
            }
        })
    }

}