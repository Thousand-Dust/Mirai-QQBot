package com.qianyun.ly

/*
 * @author Thousand_Dust
 */

interface ApiCallback1<T> {
    fun onFail(e: Exception)
    fun onSuccess(response: T)
}

interface ApiCallback2<T, K> {
    fun onFail(e: Exception)
    fun onSuccess(response1: T, response2: K)
}