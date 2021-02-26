package com.lt.retrofit.sca.test.activity

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.lt.retrofit.sca.test.R
import com.lt.retrofit.sca.test.http.InitRetrofit

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val initHttpFunctions = InitRetrofit.initHttpFunctions()
        Log.e("lllttt",initHttpFunctions.`user$login`("11","22").toString())
        initHttpFunctions.`user$login`("123","345").execute()
    }

}