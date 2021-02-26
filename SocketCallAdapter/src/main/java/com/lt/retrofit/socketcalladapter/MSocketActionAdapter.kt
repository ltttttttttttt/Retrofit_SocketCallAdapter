package com.lt.retrofit.socketcalladapter

import com.xuhao.didi.core.pojo.OriginalData
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo
import com.xuhao.didi.socket.client.sdk.client.action.SocketActionAdapter

/**
 * creator: lt  2021/2/26  lt.dygzs@qq.com
 * effect :
 * warning:
 */
internal class MSocketActionAdapter(private val adapter: SocketCallAdapter) : SocketActionAdapter() {
    override fun onSocketReadResponse(info: ConnectionInfo?, action: String?, data: OriginalData?) {
        adapter.handlerResponse(data)
    }
}