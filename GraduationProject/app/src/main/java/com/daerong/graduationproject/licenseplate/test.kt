package com.daerong.graduationproject.licenseplate

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
    print(getOcrString("룰1223라2333"))
    print(isCorrectNum(getOcrString("룰1223라2333")))
}