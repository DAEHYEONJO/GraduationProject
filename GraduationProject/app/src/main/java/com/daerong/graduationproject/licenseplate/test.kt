package com.daerong.graduationproject.licenseplate

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private fun getOcrString(str: String): String {
    val r = """[/^\d{3}[가-힣]\d{4}/]"""
    val regex = """[^가-힣0-9]""".toRegex()
    val result = str.replace(regex, "")
    var startIndex = 0
    for ((i, c) in result.withIndex()) {
        if (c.isDigit()) {
            startIndex = i
            break
        }
    }
    var lastIndex = result.length - 1
    for (i in result.length - 1 downTo startIndex + 1) {
        if (result[i].isDigit()) {
            lastIndex = i
            break
        }
    }
    return result.substring(startIndex, lastIndex + 1)
}

private fun isCorrectNum(str: String): Boolean {
    var korCount = 0
    if (str.length in 7..8) {
        str.forEach {
            if (!it.isLetterOrDigit()) return false
            if (it.isLetter()) korCount++
            if (korCount > 1) return false
        }
    } else {
        return false
    }
    return true
}

fun main() {

    val myRunnable = MyRunnable()
    val t = Thread(myRunnable)
    t.start()
    println("${Thread.currentThread().name}")
}

class MyRunnable : Runnable{
    val mutex = Mutex()
    override fun run() {
        val scope = CoroutineScope(Dispatchers.Default)
        for (i in 0 .. 100){
            scope.launch {
                println("${Thread.currentThread().name } $i")
                Thread.sleep(1000)
            }
        }
    }
}

fun fibonacci(x : Int):Int{
    if (x == 1 || x == 2) return 1
    return fibonacci(x-1) + fibonacci(x-2)
}
