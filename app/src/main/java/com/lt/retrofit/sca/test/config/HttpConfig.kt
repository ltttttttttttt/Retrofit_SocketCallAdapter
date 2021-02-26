package com.lt.retrofit.sca.test.config

/**
 * creator: lt  2019/8/8--13:30    lt.dygzs@qq.com
 * effect : 网络配置
 * warning:
 */
object HttpConfig {

    const val MODE = "http://" //请求方式

    const val TEST_HOSTNAME = "127.0.0.1:9999" //测试域名:端口
    const val TEST_URL = "" //测试服务器后缀

    @JvmStatic
    val ROOT_URL = StringBuilder(MODE)
            .append(TEST_HOSTNAME)
            .append(TEST_URL) //服务器地址
}