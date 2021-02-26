package com.lt.retrofit.socketcalladapter

import okhttp3.Call
import okhttp3.Request

open class SocketCallAdapter : Call.Factory {
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
}