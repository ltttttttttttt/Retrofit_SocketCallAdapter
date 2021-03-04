package com.lt.retrofit.socketcalladapter

import com.lt.retrofit.socketcalladapter.util.getReturnData
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager
import okio.Timeout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.lang.reflect.Method
import java.net.SocketTimeoutException
import java.util.concurrent.Executors

/**
 * creator: lt  2021/2/27  lt.dygzs@qq.com
 * effect : 用于Socket请求的Call
 * warning:
 */
internal class SocketCall<T>(
        private val manager: IConnectionManager,
        private val adapter: SocketAdapter,
        private val url: String,
        private val tMap: HashMap<String, Any>,
        private val retrofit: Retrofit,
        private val method: Method,
) : Call<T> {
    private var canceled = false
    private var isExecuted = false//是否运行过,一个call对象只允许运行一次
    private var requestId = 0

    /**
     * 检查网络三十秒,如果没有连接成功就 sync抛异常,async就回调false
     * 传入[asyncCallback]表示async
     */
    private fun checkConnect(asyncCallback: ((Boolean) -> Unit)? = null) {
        if (manager.isConnect) {
            asyncCallback?.invoke(true)
            return
        }
        adapter.connectSocket()
        val time = System.currentTimeMillis()
        if (asyncCallback == null) {
            while (true) {
                if (adapter.socketIsConnect())
                    return
                if (System.currentTimeMillis() - time > adapter.netTimeOut)
                    throw SocketTimeoutException()
                try {
                    Thread.sleep(100)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            var threadPool = adapter.threadPoolExecutor
            if (threadPool == null) {
                threadPool = Executors.newCachedThreadPool()
                adapter.setThreadPoolExecutor(threadPool)
            }
            threadPool!!.execute {
                while (true) {
                    if (adapter.socketIsConnect()) {
                        asyncCallback(true)
                        return@execute
                    }
                    if (System.currentTimeMillis() - time > adapter.netTimeOut) {
                        asyncCallback(false)
                        return@execute
                    }
                    try {
                        Thread.sleep(100)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    /**
     * 同步阻塞式请求
     */
    override fun execute(): Response<T> {
        checkConnect()
        if (isExecuted) throw IllegalStateException("只能执行一次")
        isExecuted = true
        var bytes: ByteArray? = null
        var t: Throwable? = null
        var notFinish = true
        adapter.createSendDataAndId(url, tMap) { data, id ->
            requestId = id
            adapter.addListener(requestId) { any: ByteArray, throwable: Throwable? ->
                bytes = any
                t = throwable
                notFinish = false
            }
            //发送请求
            manager.send(data)
        }
        var whileNumber = 0
        while (notFinish) {
            try {
                whileNumber++
                if (whileNumber % 20 == 0)
                    adapter.handlerTimeOutedListener()
                Thread.sleep(50)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        t?.let { throw it }
        return Response.success(bytes.getReturnData(retrofit, method)) as Response<T>
    }

    /**
     * 异步请求
     */
    override fun enqueue(callback: Callback<T>) {
        checkConnect {
            if (!it) {
                adapter.handlerCallbackRunnable {
                    callback.onFailure(this, SocketTimeoutException())
                }
                return@checkConnect
            }
            if (isExecuted) throw IllegalStateException("只能执行一次")
            isExecuted = true
            adapter.createSendDataAndId(url, tMap) { data, id ->
                requestId = id
                adapter.addListener(requestId) { bytes: ByteArray, throwable: Throwable? ->
                    callback.onResponse(this, Response.success(bytes.getReturnData(retrofit, method)) as Response<T>)
                }
                //发送请求
                manager.send(data)
            }
        }
    }

    override fun clone(): Call<T> = SocketCall(manager, adapter, url, tMap, retrofit, method)

    override fun isExecuted(): Boolean = isExecuted

    override fun cancel() {
        canceled = true
        // 取消请求(移除回调)
        adapter.removeListener(requestId)
    }

    override fun isCanceled(): Boolean = canceled

    override fun request() = null

    override fun timeout(): Timeout = Timeout.NONE

}