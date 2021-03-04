package com.lt.retrofit.socketcalladapter.util

/**
 * creator: lt  2021/2/27  lt.dygzs@qq.com
 * effect : list扩展方法
 * warning:
 */

/**
 * map移除lambda中返回true的元素
 * ps:会改变自身的数据,跟MutableList.removeAll方法相同
 */
inline fun <K, V> MutableMap<K, V>.whileThis(checkRemove: (MutableMap.MutableEntry<K, V>) -> Boolean) {
    entries.filter(checkRemove).forEach {
        remove(it.key)
    }
}

/**
 * 使用List内的数据逐个尝试执行操作并创建对象,直到返回不为空的对象则停止遍历
 */
inline fun <T, R : Any> List<T>.tryCreate(run: (T) -> R?): R? {
    var r: R? = null
    for (t in this) {
        r = run(t)
        if (r != null)
            break
    }
    return r
}