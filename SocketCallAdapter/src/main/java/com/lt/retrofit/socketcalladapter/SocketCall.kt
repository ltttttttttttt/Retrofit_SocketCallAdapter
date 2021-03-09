package com.lt.retrofit.socketcalladapter

import com.lt.retrofit.socketcalladapter.util.createCancelException
import com.lt.retrofit.socketcalladapter.util.getReturnData
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager
import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.lang.reflect.Method
import java.net.SocketTimeoutException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * creator: lt  2021/2/27  lt.dygzs@qq.com
 * effect : 用于Socket请求的Call
 * warning:
 */
open class SocketCall<T>(
        val manager: IConnectionManager,
        val adapter: SocketAdapter,
        val url: String,
        val tMap: HashMap<String, Any>,
        val retrofit: Retrofit,
        val method: Method,
) : Call<T> {
    private var canceled = false
    private var isExecuted = false//是否运行过,一个call对象只允许运行一次
    private var requestId = 0

    /**
     * 检查网络三十秒,如果没有连接成功就 sync抛异常,async就回调false
     * [asyncCallback]返回null表示没有异常,成功;否则有异常
     */
    private inline fun checkConnect(isAsync: Boolean = false, crossinline asyncCallback: (Exception?) -> Unit = { }) {
        if (adapter.socketIsConnect()) {
            asyncCallback.invoke(null)
            return
        }
        adapter.connectSocket()
        val time = System.currentTimeMillis()
        if (!isAsync) {
            while (true) {
                if (adapter.socketIsConnect())
                    return
                if (System.currentTimeMillis() - time > adapter.netTimeOut)
                    throw SocketTimeoutException()
                try {
                    Thread.sleep(100)
                } catch (e: Exception) {
                    throw createCancelException(e)
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
                        asyncCallback(null)
                        return@execute
                    }
                    if (System.currentTimeMillis() - time > adapter.netTimeOut) {
                        asyncCallback(SocketTimeoutException())
                        return@execute
                    }
                    try {
                        Thread.sleep(100)
                    } catch (e: Exception) {
                        asyncCallback(createCancelException(e))
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
                //两秒检查一次超时
                if (whileNumber % 40 == 0)
                    adapter.handlerTimeOutedListener()
                Thread.sleep(50)
            } catch (e: Exception) {
                adapter.removeListener(requestId)
                throw createCancelException(e)
            }
        }
        t?.let { throw it }
        return Response.success(bytes.getReturnData(retrofit, method)) as Response<T>
    }

    /**
     * 异步请求
     */
    override fun enqueue(callback: Callback<T>) {
        checkConnect(true) {
            if (it != null) {
                adapter.handlerCallbackRunnable {
                    callback.onFailure(this, it)
                }
                return@checkConnect
            }
            if (isExecuted) throw IllegalStateException("只能执行一次")
            isExecuted = true
            adapter.createSendDataAndId(url, tMap) { data, id ->
                requestId = id
                adapter.addListener(requestId) { bytes: ByteArray, throwable: Throwable? ->
                    if (throwable == null)
                        callback.onResponse(this, Response.success(bytes.getReturnData(retrofit, method)) as Response<T>)
                    else
                        callback.onFailure(this, throwable)
                }
                //发送请求
                manager.send(data)
                //处理超时
                var timeThreadExecutor = adapter.timeThreadExecutor
                if (timeThreadExecutor == null) {
                    val threadPool = Executors.newScheduledThreadPool(1)
                    timeThreadExecutor = threadPool
                    adapter.timeThreadExecutor = threadPool
                }
                timeThreadExecutor!!.schedule({
                    adapter.handlerTimeOutedListener()
                }, adapter.netTimeOut + 100, TimeUnit.MILLISECONDS)
            }
        }
    }

    override fun clone(): SocketCall<T> = SocketCall(manager, adapter, url, tMap, retrofit, method)

    override fun isExecuted(): Boolean = isExecuted

    override fun cancel() {
        canceled = true
        // 取消请求(移除回调)
        adapter.removeListener(requestId)
    }

    override fun isCanceled(): Boolean = canceled

    override fun request(): Request? = null

    override fun timeout(): Timeout = Timeout.NONE
}