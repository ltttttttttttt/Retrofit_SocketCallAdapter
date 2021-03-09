package com.lt.retrofit.socketcalladapter.util

import java.io.IOException

/**
 * creator: lt  2021/3/9  lt.dygzs@qq.com
 * effect : 异常工具类
 * warning:
 */

/**
 * 创建IO的取消异常
 */
fun createCancelException(superException: Exception? = null) = IOException("Canceled", superException)