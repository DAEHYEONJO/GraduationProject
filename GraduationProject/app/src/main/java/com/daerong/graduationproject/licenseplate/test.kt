package com.daerong.graduationproject.licenseplate

import java.util.concurrent.TimeUnit

fun main() {
    val regex = """[^가-힣0-9]""".toRegex()
    var result = "끄겜겜"
    var str2 = "^^~~~1@!:::"
    result = result.replace(regex,"")

    result.forEach {
        if (it.isLetterOrDigit()){
            println("$it is letterorDigit")
        }else{
            println("$it 이거나옴안됨")
        }
    }
    var startIndex = 0
    for ((i,c) in result.withIndex()){
        if (c.isDigit()){
            startIndex = i
            break
        }
    }
    var lastIndex = result.length-1
    for(i in result.length-1 downTo startIndex+1){
        if (result[i].isDigit()){
            lastIndex = i
            break
        }
    }

    val resultStr = result.substring(startIndex,lastIndex+1)
    println(resultStr)


    var lastTimeStamp = 0L
    while (true) {
        var curTime = System.currentTimeMillis()
        if (curTime-lastTimeStamp>=TimeUnit.SECONDS.toMillis(1)){
            println(lastTimeStamp)
            println(TimeUnit.SECONDS.toMillis(1))
            lastTimeStamp = curTime
        }

    }

}