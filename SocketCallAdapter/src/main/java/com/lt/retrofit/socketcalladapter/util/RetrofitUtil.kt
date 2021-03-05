package com.lt.retrofit.socketcalladapter.util

import okhttp3.MediaType
import okhttp3.ResponseBody
import retrofit2.Retrofit
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

/**
 * creator: lt  2021/3/4  lt.dygzs@qq.com
 * effect : Retrofit相关工具类
 * warning:
 */

/**
 * 使用Retrofit的converterFactorie来将数据生成对象
 */
fun ByteArray?.getReturnData(retrofit: Retrofit, method: Method): Any? {
    this ?: return null
    return try {
        retrofit.converterFactories().tryCreate {
            it.responseBodyConverter(
                    getParameterUpperBound(0, method.genericReturnType as ParameterizedType),
                    method.annotations,
                    retrofit
            )?.convert(ResponseBody.create(MediaType.get("text/plain"), this))
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 获取type对应索引的泛型的type
 */
private fun getParameterUpperBound(index: Int, type: ParameterizedType): Type {
    val types = type.actualTypeArguments
    require(!(index < 0 || index >= types.size)) { "Index " + index + " not in range [0," + types.size + ") for " + type }
    val paramType = types[index]
    return if (paramType is WildcardType) {
        paramType.upperBounds[0]
    } else paramType
}