package com.lt.retrofit.sca.test.http

import com.lt.retrofit.sca.test.model.IpBean
import com.lt.retrofit.sca.test.model.NetBean
import retrofit2.Call

/**
 * creator: lt  2020/9/23  lt.dygzs@qq.com
 * effect : 网络请求方法
 * warning:
 */
interface HttpFunctions {

    /**
     * 登陆
     * @param userName 登陆名
     * @param password  密码
     */
    fun `user$login`(
            userName: String,
            password: String,
    ): Call<NetBean<IpBean>>
}