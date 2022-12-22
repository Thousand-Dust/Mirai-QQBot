package com.qqbot

import kotlinx.coroutines.*

fun main() {
    runBlocking(Dispatchers.IO) {
        for (i in 1..10) {
            test()
        }
    }
}

suspend fun test() {
    Thread.sleep(1000)
    delay(1000)
    println("Hello World")
}