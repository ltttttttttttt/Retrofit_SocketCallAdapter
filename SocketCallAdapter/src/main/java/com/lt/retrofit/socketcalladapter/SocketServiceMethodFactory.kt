package com.lt.retrofit.socketcalladapter

import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager
import retrofit2.OtherServiceMethod
import retrofit2.RequestFactory
import retrofit2.Retrofit
import java.lang.reflect.Method

/**
 * creator: lt  2021/3/4  lt.dygzs@qq.com
 * effect : 用于生成ServiceMethod来适配动态代理的Method
 * warning:
 * [manager]OkSocket的管理器
 * [adapter]使用者需要继承SocketCallAdapter实现对应功能
 */
open class SocketServiceMethodFactory(
        private val manager: IConnectionManager,
        private val adapter: SocketAdapter,
) : OtherServiceMethod.Factory<Any?> {
    override fun createServiceMethod(retrofit: Retrofit,
                                     method: Method,
                                     requestFactory: RequestFactory): OtherServiceMethod<Any?>? {
        return SocketServiceMethod(
                manager,
                adapter,
                retrofit,
                method,
                requestFactory
        )
    }
}