package com.lt.retrofit.socketcalladapter

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okio.Timeout
import java.io.IOException

/**
 * creator: lt  2021/2/27  lt.dygzs@qq.com
 * effect : 用于包装RetrofitCall为OkHttpCall
 * warning:
 */
internal class OkHttpCallWithRetrofitCall(
        private val call: retrofit2.Call<Any?>,
        private val adapter: SocketCallAdapter
) : Call {
    override fun clone(): Call = OkHttpCallWithRetrofitCall(call, adapter)

    override fun request(): Request = call.request()

    override fun execute(): Response = call.execute().raw()

    override fun enqueue(responseCallback: Callback) {
        call.enqueue(object : retrofit2.Callback<Any?> {
            override fun onResponse(call: retrofit2.Call<Any?>, response: retrofit2.Response<Any?>) {
                adapter.handlerCallbackRunnable {
                    responseCallback.onResponse(this@OkHttpCallWithRetrofitCall, response.raw())
                }
            }

            override fun onFailure(call: retrofit2.Call<Any?>, t: Throwable) {
                adapter.handlerCallbackRunnable {
                    responseCallback.onFailure(this@OkHttpCallWithRetrofitCall, IOException(t))
                }
            }
        })
    }

    override fun cancel() = call.cancel()

    override fun isExecuted(): Boolean = call.isExecuted

    override fun isCanceled(): Boolean = call.isCanceled

    override fun timeout(): Timeout = call.timeout()
}