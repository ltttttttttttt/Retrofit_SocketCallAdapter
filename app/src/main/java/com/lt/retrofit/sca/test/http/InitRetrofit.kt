package com.lt.retrofit.sca.test.http

import com.google.gson.Gson
import com.lt.retrofit.sca.test.config.HttpConfig
import com.lt.retrofit.sca.test.model.PulseData
import com.lt.retrofit.sca.test.model.TcpSendData
import com.lt.retrofit.sca.test.util.ByteUtils
import com.lt.retrofit.socketcalladapter.SocketCallAdapter
import com.xuhao.didi.core.iocore.interfaces.ISendable
import com.xuhao.didi.core.pojo.OriginalData
import com.xuhao.didi.core.protocol.IReaderProtocol
import com.xuhao.didi.socket.client.sdk.OkSocket
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions
import com.xuhao.didi.socket.client.sdk.client.action.SocketActionAdapter
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger

/**
 * creator: lt  2020/9/23  lt.dygzs@qq.com
 * effect : 初始化和配置Retrofit
 * warning:
 */
object InitRetrofit {

    internal fun initHttpFunctions(): HttpFunctions {
//        val builder = OkHttpClient.Builder()
//        builder.connectTimeout(30L, TimeUnit.SECONDS)
//                .readTimeout(30L, TimeUnit.SECONDS)
//                .writeTimeout(30L, TimeUnit.SECONDS)
//                .connectionPool(ConnectionPool(5, 300, TimeUnit.SECONDS))
//
//        val client = builder.build()

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
        val gson = Gson()

        val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(HttpConfig.ROOT_URL.toString())
                .addConverterFactory(GsonConverterFactory.create())
//                .client(client)
                .callFactory(object : SocketCallAdapter(manager) {
                    override fun getResponseIdAndAny(data: OriginalData, getTypeFun: (id: Int) -> Type?): Pair<Int, Any?>? {
                        val jb = JSONObject(String(data.bodyBytes))
                        if (!jb.has("id")) return null
                        val id = jb.getInt("id")
                        val type = getTypeFun(id) ?: return null
                        return id to gson.fromJson(jb.optString("body"), type)
                    }

                    override fun socketIsConnect(): Boolean = manager.isConnect

                    override fun createSendDataAndId(url: String, requestParametersMap: HashMap<String, Any>, returns: (ISendable, Int) -> Unit) {
                        val id = mId.incrementAndGet()
                        val data = TcpSendData(id, url, requestParametersMap)
                        returns(data, id)
                    }

                })
                .build()

        return retrofit.create(HttpFunctions::class.java)
    }
}