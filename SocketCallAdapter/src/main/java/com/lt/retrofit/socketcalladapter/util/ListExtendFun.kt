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