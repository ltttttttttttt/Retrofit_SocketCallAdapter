package com.lt.retrofit.sca.test.activity

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.lt.retrofit.sca.test.R
import com.lt.retrofit.sca.test.http.InitRetrofit
import com.lt.retrofit.sca.test.util.e
import retrofit2.Call

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val initHttpFunctions = InitRetrofit.initHttpFunctions()
        val call = initHttpFunctions.`user$login`("11", "22")
        call.e()
        val declaredField = call::class.java.getDeclaredField("delegate")
        declaredField.isAccessible = true
        val get = declaredField.get(call) as Call<*>
        get.e()
        val declaredField1 = get::class.java.getDeclaredField("rawCall")
        declaredField1.isAccessible = true
        val call1 = declaredField1.get(get) as okhttp3.Call
        call1.e()
        Log.e("lllttt", initHttpFunctions.`user$login`("123", "345").execute().toString())
    }

}