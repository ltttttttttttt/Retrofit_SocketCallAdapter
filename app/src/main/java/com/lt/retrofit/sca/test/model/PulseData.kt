package com.lt.retrofit.sca.test.model

import com.xuhao.didi.core.iocore.interfaces.IPulseSendable

class PulseData : IPulseSendable {
    override fun parse(): ByteArray {
        return """{"id":0,"url":"ping"}""".toByteArray()
    }
}