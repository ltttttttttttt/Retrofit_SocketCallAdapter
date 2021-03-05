package com.lt.retrofit.socketcalladapter

import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager
import okhttp3.HttpUrl
import retrofit2.Call
import retrofit2.OtherServiceMethod
import retrofit2.RequestFactory
import retrofit2.Retrofit
import java.lang.reflect.Method

/**
 * creator: lt  2021/3/4  lt.dygzs@qq.com
 * effect : 适配动态代理的Method
 * warning:
 */
open class SocketServiceMethod(
        private val manager: IConnectionManager,
        private val adapter: SocketAdapter,
        retrofit: Retrofit,
        method: Method,
        requestFactory: RequestFactory,
) : OtherServiceMethod<Any?>(retrofit, method, requestFactory) {
    override fun createCall(url: HttpUrl, requestParameterMap: Map<String?, Any?>?, args: Array<out Any>): Call<Any?> {
        val tMap = HashMap<String, Any>()
        requestParameterMap?.entries?.forEach { (k, v) ->
            tMap[k ?: ""] = v ?: ""
        }
        return SocketCall(manager, adapter, url.toString(), tMap, retrofit, method)
    }
}