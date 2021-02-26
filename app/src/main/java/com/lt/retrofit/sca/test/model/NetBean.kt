package com.lt.retrofit.sca.test.model

/**
 * creator: lt  2020/9/26  lt.dygzs@qq.com
 * effect : 网络请求bean的base类
 * warning:
 */
class NetBean<T>(
        val data: T? = null,
        val msg: String? = null,
        val code: Int = 0
)