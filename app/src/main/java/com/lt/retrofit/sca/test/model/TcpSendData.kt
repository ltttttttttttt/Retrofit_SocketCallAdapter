package com.lt.retrofit.sca.test.model

import com.xuhao.didi.core.iocore.interfaces.ISendable
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

open class TcpSendData(id: Int, path: String, dataMap: Map<String, Any>) : ISendable {
    private val jsonString: String

    init {
        val jsonObject = JSONObject()
        jsonObject.put("msg_type", path)
        jsonObject.put("request_id", id)
        jsonObject.put("headers", getHeader(path))
        val bodyObject = JSONObject()
        dataMap.forEach {
            bodyObject.put(it.key, it.value)
        }
        jsonObject.put("body", bodyObject)

        jsonString = jsonObject.toString()
    }

    override fun parse(): ByteArray {
        //根据服务器的解析规则,构建byte数组
        val body = jsonString.toByteArray(Charset.defaultCharset())
        val bb = ByteBuffer.allocate(4 + body.size)
        bb.order(ByteOrder.BIG_ENDIAN)
        bb.putInt(body.size)
        bb.put(body)
        return bb.array()
    }

    private fun getHeader(path: String): JSONObject {
        val headerObject = JSONObject()
        headerObject.put("timestamp", System.currentTimeMillis())
        headerObject.put("path", path)
        return headerObject
    }
}