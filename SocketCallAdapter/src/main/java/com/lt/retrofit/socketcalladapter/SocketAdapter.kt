package com.lt.retrofit.socketcalladapter

import com.lt.retrofit.socketcalladapter.util.whileThis
import com.xuhao.didi.core.iocore.interfaces.ISendable
import com.xuhao.didi.core.pojo.OriginalData
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo
import com.xuhao.didi.socket.client.sdk.client.action.SocketActionAdapter
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager
import okhttp3.FormBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

/**
 * creator: lt  2021/2/26  lt.dygzs@qq.com
 * effect : 构造出用于请求Socket的Call,可以直接以Retrofit的方式使用
 * warning:
 * [manager]OkSocket的管理器
 * [useNewReceiver]如果为true就在该类内部绑定一个新的receiver,数据可能会被处理多遍,较为浪费性能,但是不用进行配置
 *                  如果为false就需要用户手动在收到服务端数据推送后调用[handlerCallback]方法,可以优化性能
 *                  且为false可以直接重写方法[getResponseIdAndBodyBytes]并返回null即可,因为其不会被回调了
 */
abstract class SocketAdapter(
        private val manager: IConnectionManager,
        private val useNewReceiver: Boolean = true,
) {
    //用于接收OkSocket返回的数据
    private val receiver = if (!useNewReceiver) null else object : SocketActionAdapter() {
        override fun onSocketReadResponse(info: ConnectionInfo?, action: String?, data: OriginalData?) {
            handlerResponse(data)
        }
    }

    //回调的map,超时x秒 Map<请求id,Pair<插入时间,回调(返回的body字节数据,异常)>>
    private val listenerMap = ConcurrentHashMap<Int, Pair<Long, (ByteArray, Throwable?) -> Unit>>()
    internal var threadPoolExecutor: ExecutorService? = null//内部使用的子线程线程池
    internal var netTimeOut = 30000L//网络超时时间,默认半分钟
    private val encodedNamesField by lazy { FormBody::class.java.getDeclaredField("encodedNames").apply { isAccessible = true } }//post的keys反射对象
    private val encodedValuesField by lazy { FormBody::class.java.getDeclaredField("encodedValues").apply { isAccessible = true } }//post的values反射对象

    init {
        if (receiver != null)
            manager.registerReceiver(receiver)
    }

    /**
     * 从响应数据中获取请求id
     * [data]OkSocket返回的字节数据
     * 如果返回null表示识别不了该数据(或该数据是推送,不是响应)
     * 返回对应的id和body数据对应的字节数组
     */
    abstract fun getResponseIdAndBodyBytes(data: OriginalData): Pair<Int, ByteArray>?

    /**
     * 返回当前Socket在逻辑意义上是否和服务端连通了
     */
    abstract fun socketIsConnect(): Boolean

    /**
     * 根据url和请求参数生成用于发送的数据和Id
     * [url]请求的url
     * [requestParametersMap]请求参数的map
     * [returns]把请求对象和id创建完成后调用其invoke方法表示已经创建完成
     */
    abstract fun createSendDataAndId(url: String, requestParametersMap: HashMap<String, Any>, returns: (ISendable, Int) -> Unit)

    /**
     * 当网络断开后自动连接网络,可以重写此方法来控制连接流程
     */
    open fun connectSocket() {
        cancelAllListener()
        manager.connect()
    }

    /**
     * 处理回调在哪个线程执行,可以通过重写此方法来控制,默认OkSocket内的子线程
     * [runnable]需要执行的代码
     */
    open fun handlerCallbackRunnable(runnable: Runnable) {
        runnable.run()
    }

    /**
     * 设置网络请求超时时间
     * [time]超时时间,单位毫秒
     */
    fun setNetTimeOut(time: Long): SocketAdapter {
        netTimeOut = time
        return this
    }

    /**
     * 设置内部使用的子线程的线程池,如果不设置则会使用默认线程池
     * [threadPoolExecutor]子线程线程池
     */
    fun setThreadPoolExecutor(threadPoolExecutor: ExecutorService): SocketAdapter {
        this.threadPoolExecutor = threadPoolExecutor
        return this
    }

    /**
     * 销毁自身并和Socket解除绑定
     */
    fun destroy(): SocketAdapter {
        if (receiver != null)
            manager.unRegisterReceiver(receiver)
        cancelAllListener()
        return this
    }

    /**
     * 取消所有无效的回调,用于重新连接socket
     * 可以在重新连接后调用,将之前已经不可能收到的回调清除掉
     */
    fun cancelAllListener() {
        listenerMap.whileThis {
            it.value.second(ByteArray(0), IOException("Canceled"))
            true
        }
    }

    /**
     * 在获取到服务端的响应后处理数据和回调
     * [id]请求id
     * [bodyBytes]返回的内容的字节数组
     */
    fun handlerCallback(id: Int, bodyBytes: ByteArray) {
        val (_, listener) = listenerMap.remove(id) ?: return
        handlerCallbackRunnable {
            listener(bodyBytes, null)
        }
        handlerTimeOutedListener()
    }

    //处理收到的数据(只处理有回调的)
    internal fun handlerResponse(data: OriginalData?) {
        data ?: return
        try {
            val mData = getResponseIdAndBodyBytes(data) ?: return
            handlerCallback(mData.first, mData.second)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    //添加回调
    internal fun addListener(id: Int, listener: (ByteArray, Throwable?) -> Unit) {
        handlerTimeOutedListener()
        listenerMap[id] = Pair(System.currentTimeMillis(), listener)
    }

    //移除回调
    internal fun removeListener(id: Int) {
        listenerMap.remove(id)
        handlerTimeOutedListener()
    }

    //处理超时的回调,将其从map中移除并回调一个SocketTimeoutException
    internal fun handlerTimeOutedListener() {
        val time = System.currentTimeMillis()
        listenerMap.whileThis {
            if (time - it.value.first > netTimeOut) {
                handlerCallbackRunnable {
                    it.value.second(ByteArray(0), SocketTimeoutException())
                }
                return@whileThis true
            }
            false
        }
    }
}