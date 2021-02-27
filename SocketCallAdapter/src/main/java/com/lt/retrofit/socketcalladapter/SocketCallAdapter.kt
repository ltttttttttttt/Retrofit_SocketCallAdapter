package com.lt.retrofit.socketcalladapter

import com.lt.retrofit.socketcalladapter.util.Pair3
import com.lt.retrofit.socketcalladapter.util.whileThis
import com.xuhao.didi.core.iocore.interfaces.ISendable
import com.xuhao.didi.core.pojo.OriginalData
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo
import com.xuhao.didi.socket.client.sdk.client.action.SocketActionAdapter
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager
import okhttp3.Call
import okhttp3.Request
import java.lang.reflect.Type
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

/**
 * creator: lt  2021/2/26  lt.dygzs@qq.com
 * effect : 构造出用于请求Socket的Call,可以直接以Retrofit的方式使用
 * warning: 返回的Call会回调(或抛出)以下异常:
 *              1.SocketTimeoutException:表示网络请求超时
 *              2.NullPointerException:表示返回的数据为空
 *              3.CancellationException:表示由于重新连接了,所以之前的回调都变为了无效回调(因为收不到相应的消息了),所以表示其被取消
 *              4.其他非主观异常
 */
abstract class SocketCallAdapter(private val manager: IConnectionManager) : Call.Factory {
    //回调的map,超时x秒 Map<请求id,Pair<插入时间,返回对象的type,回调(对象,异常)>>
    private val listenerMap = ConcurrentHashMap<Int, Pair3<Long, Type, (Any?, Throwable?) -> Unit>>()

    //用于接收OkSocket返回的数据
    private val receiver = object : SocketActionAdapter() {
        override fun onSocketReadResponse(info: ConnectionInfo?, action: String?, data: OriginalData?) {
            handlerResponse(data)
        }
    }
    internal var threadPoolExecutor: ExecutorService? = null//内部使用的子线程线程池
    internal var netTimeOut = 30000L//网络超时时间

    init {
        manager.registerReceiver(receiver)
    }

    /**
     * 从响应数据中获取请求id和相应的返回对象
     * [data]OkSocket返回的字节数据
     * [getTypeFun]调用其invoke方法通过id获取其对应需要生成的对象的Type
     * 如果返回null表示识别不了该数据(或该数据是推送,不是响应)
     */
    abstract fun getResponseIdAndAny(data: OriginalData, getTypeFun: (id: Int) -> Type?): Pair<Int, Any?>?

    /**
     * 返回当前Socket在逻辑意义上是否和服务端联通了
     */
    abstract fun socketIsConnect(): Boolean

    /**
     * 根据url和请求参数
     * [url]请求的url
     * [requestParametersMap]请求参数的map
     * [returns]把请求对象和id创建完成后调用其invoke方法表示已经创建完成
     */
    abstract fun createSendDataAndId(url: String, requestParametersMap: HashMap<String, Any>, returns: (ISendable, Int) -> Unit)

    /**
     * 当网络断开后自动连接网络,可以重写此方法来控制连接流程
     */
    open fun connectSocket() {
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
     * 销毁自身并和Socket解除绑定
     */
    fun destroy() {
        manager.unRegisterReceiver(receiver)
    }

    /**
     * 设置网络请求超时时间
     * [time]超时时间,单位毫秒
     */
    fun setNetTimeOut(time: Long): SocketCallAdapter {
        netTimeOut = time
        return this
    }

    /**
     * 设置内部使用的子线程的线程池,如果不设置则会使用默认线程池
     * [threadPoolExecutor]子线程线程池
     */
    fun setThreadPoolExecutor(threadPoolExecutor: ExecutorService): SocketCallAdapter {
        this.threadPoolExecutor = threadPoolExecutor
        return this
    }

    override fun newCall(request: Request): Call {
        if (false)
            return SocketCall(
                    manager,
                    this,
                    request.url().toString(),
                    hashMapOf(),
                    String::class.java
            )

        println("******************************************************************")
        println("url=" + request.url())
        println("method=" + request.method())
        request.headers().names().forEach {
            println("headers=$it:${request.header(it)}")
        }
        println("body=" + request.body().toString())
        val declaredField = request::class.java.getDeclaredField("tags")
        declaredField.isAccessible = true
        (declaredField.get(request) as Map<Any, Any>).entries.forEach {
            println("tags=${it.key}:${it.value}")
        }
        return null!!
    }

    //处理收到的数据(只处理有回调的)
    internal fun handlerResponse(data: OriginalData?) {
        data ?: return
        try {
            val (id, responseAny) = getResponseIdAndAny(data) {
                listenerMap[it]?.two
            } ?: return
            // TODO by lt 2021/2/26 18:00 想想日志应该怎么打印
            //处理回调
            val (_, _, listener) = listenerMap.remove(id) ?: return
            try {
                handlerCallbackRunnable {
                    if (responseAny == null) listener(null, NullPointerException()) else listener(responseAny, null)
                }
            } catch (t: Throwable) {
                handlerCallbackRunnable {
                    listener(null, t)
                }
            }
            handlerTimeOutedListener()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    //添加回调
    internal fun addListener(id: Int, type: Type, listener: (Any?, Throwable?) -> Unit) {
        handlerTimeOutedListener()
        listenerMap[id] = Pair3(System.currentTimeMillis(), type, listener)
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
            if (time - it.value.one > netTimeOut) {
                handlerCallbackRunnable {
                    it.value.three(null, SocketTimeoutException())
                }
                return@whileThis true
            }
            false
        }
    }
}