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
 * [useNewReceiver]如果为true就在该类内部绑定一个新的receiver,数据可能会被处理多遍,较为浪费性能,但是不用进行配置
 *                  如果为false就需要用户手动在收到服务端数据推送后调用[handlerCallback]方法,可以优化性能
 */
class SocketServiceMethodFactory(
        private val manager: IConnectionManager,
        private val adapter: SocketAdapter,
        private val useNewReceiver: Boolean = true,
) : OtherServiceMethod.Factory<Any?> {
    override fun createServiceMethod(retrofit: Retrofit,
                                     method: Method,
                                     requestFactory: RequestFactory): OtherServiceMethod<Any?> {
        return SocketServiceMethod(
                manager,
                adapter,
                retrofit,
                method,
                requestFactory
        )
    }
}