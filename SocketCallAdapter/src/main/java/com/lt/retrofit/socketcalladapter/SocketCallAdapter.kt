package com.lt.retrofit.socketcalladapter

import com.lt.retrofit.socketcalladapter.util.Pair3
import com.xuhao.didi.core.pojo.OriginalData
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager
import okhttp3.Call
import okhttp3.Request
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap

/**
 * creator: lt  2021/2/26  lt.dygzs@qq.com
 * effect : 构造出用于请求Socket的Call,可以直接以Retrofit的方式使用
 * warning:
 */
abstract class SocketCallAdapter(private val manager: IConnectionManager) : Call.Factory {
    //回调的map,超时x秒 Map<请求id,Pair<插入时间,返回对象的type,回调(对象,异常)>>
    private val listenerMap = ConcurrentHashMap<Int, Pair3<Long, Type, (Any?, Throwable?) -> Unit>>()
    private val receiver = MSocketActionAdapter(this)

    init {
        manager.registerReceiver(receiver)
    }

    /**
     * 从返回数据中获取请求id和相应的返回对象
     */
    abstract fun getIdAndAny(data: OriginalData, getTypeFun: (id: Int) -> Type?): Pair<Int, Any?>

    /**
     * 销毁自身并和Socket解除绑定
     */
    fun destroy() {
        manager.unRegisterReceiver(receiver)
    }

    override fun newCall(request: Request): Call {
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
        println("request=$request")
        return null!!
    }

    internal fun handlerResponse(data: OriginalData?) {
        data ?: return
        try {
            val (id, responseAny) = getIdAndAny(data) {
                listenerMap[it]?.two
            }
            if (id > 0) {// TODO by lt 2021/2/26 18:00 id需要大于0 ,想想日志应该怎么打印
                //处理回调
                val (_, type, listener) = listenerMap.remove(requestId) ?: return
                try {
                    val any = Gson().fromJson<Any?>(body.getJSONObject("body").toString(), type)
                    HandlerPool.post {
                        if (any == null) listener(null, ServerException("服务端返回的result是空")) else listener(any, null)
                    }
                } catch (t: Throwable) {
                    HandlerPool.post {
                        listener(null, t)
                    }
                }
                handlerTimeOutedListener()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}