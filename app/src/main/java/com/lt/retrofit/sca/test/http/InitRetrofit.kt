package com.lt.retrofit.sca.test.http

import android.os.Handler
import android.os.Looper
import com.lt.retrofit.sca.test.config.HttpConfig
import com.lt.retrofit.sca.test.model.TcpSendData
import com.lt.retrofit.sca.test.util.ByteUtils
import com.lt.retrofit.socketcalladapter.SocketAdapter
import com.lt.retrofit.socketcalladapter.SocketServiceMethodFactory
import com.xuhao.didi.core.iocore.interfaces.ISendable
import com.xuhao.didi.core.pojo.OriginalData
import com.xuhao.didi.core.protocol.IReaderProtocol
import com.xuhao.didi.socket.client.sdk.OkSocket
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions
import com.xuhao.didi.socket.client.sdk.client.action.SocketActionAdapter
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger

/**
 * creator: lt  2020/9/23  lt.dygzs@qq.com
 * effect : 初始化和配置Retrofit
 * warning:
 */
object InitRetrofit {

    internal fun initHttpFunctions(): HttpFunctions {
        //OkSocket管理器
        val manager = OkSocket.open(ConnectionInfo("127.0.0.1", 1234))
        //配置
        val options = manager.option
        val builder = OkSocketOptions.Builder(options)
        builder.setConnectTimeoutSecond(20)
        builder.setReadPackageBytes(32 * 1024)
        builder.setWritePackageBytes(32 * 1024)
        builder.setPulseFrequency(10 * 1000)
        builder.setPulseFeedLoseTimes(3)
        builder.setReaderProtocol(object : IReaderProtocol {
            override fun getHeaderLength(): Int = 4

            override fun getBodyLength(header: ByteArray?, byteOrder: ByteOrder?): Int = ByteUtils.bytes2Int(header)

        })//解包管理
//        builder.setReconnectionManager()//重连管理
        manager.option(builder.build())
        manager.registerReceiver(object : SocketActionAdapter() {
            override fun onSocketReadResponse(info: ConnectionInfo?, action: String?, data: OriginalData?) {
                data ?: return
                manager.pulseManager?.feed()
                //检查如果是推送可以在这里处理
            }
        })
        //只需要设置一次,下一次可以直接调用pulse()
//        manager.pulseManager.setPulseSendable(PulseData()).pulse()//开始心跳,开始心跳后,心跳管理器会自动进行心跳触发
        //连接socket
        manager.connect()
        val mId = AtomicInteger(0)
        val handler = Handler(Looper.getMainLooper())

        val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(HttpConfig.ROOT_URL.toString())
                .addConverterFactory(GsonConverterFactory.create())
                //设置socket处理不了后http的请求代理对象为OkHttpClient(这里没有判断Request的url和请求方法等,处理不了直接走http)
                .client(OkHttpClient())
                .setServiceMethodFactory(SocketServiceMethodFactory(manager, object : SocketAdapter(manager) {

                    //从服务器返回的数据中读取id和body数据
                    override fun getResponseIdAndBodyBytes(data: OriginalData): Pair<Int, ByteArray>? {
                        val jb = JSONObject(String(data.bodyBytes))
                        if (!jb.has("id")) return null
                        return jb.getInt("id") to jb.optString("body").toByteArray()
                    }

                    //只要socket连接上就认为该socket通道是通的
                    override fun socketIsConnect(): Boolean = manager.isConnect

                    //根据请求url和请求数据map来创建ISendable(OkSocket的请求数据类型)对象,并调用returns函数(为了支持异步)
                    override fun createSendDataAndId(url: String, requestParametersMap: HashMap<String, Any>, returns: (ISendable, Int) -> Unit) {
                        val id = mId.incrementAndGet()
                        val data = TcpSendData(id, url, requestParametersMap)
                        returns(data, id)
                    }

                    //在主线程回调数据
                    override fun handlerCallbackRunnable(runnable: Runnable) {
                        handler.post(runnable)
                    }
                }))
                .build()

        return retrofit.create(HttpFunctions::class.java)
    }
}