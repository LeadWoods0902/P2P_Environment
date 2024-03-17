package com.leadwoods.p2p_environment.support

import android.util.Log
import com.leadwoods.p2p_environment.support.LoggingGroups.ALWAYS
import com.leadwoods.p2p_environment.support.LoggingGroups.EVERY_TENTH
import com.leadwoods.p2p_environment.support.LoggingGroups.FIRST_ONLY
import com.leadwoods.p2p_environment.support.LoggingGroups.IF_TIME_ELAPSED_10S
import com.leadwoods.p2p_environment.support.LoggingGroups.IF_TIME_ELAPSED_1M

enum class LoggingGroups{
    ALWAYS,
    FIRST_ONLY, // onceOnlyMethods
    EVERY_TENTH, // everyTenthMethods
    IF_TIME_ELAPSED_10S, //
    IF_TIME_ELAPSED_1M
}

abstract class Logger {
    companion object {

        private var methodsOnceOnly = mutableSetOf<String>()
        private var methodsEveryTenth = mutableMapOf<String, Int>()
        private var methods10s = mutableMapOf<String, Long>()
        private var methods1m = mutableMapOf<String, Long>()

        private val lock = Any()


        private fun callingMethod(): Pair<String, String> {
            val stackTraceElement = Throwable().stackTrace.getOrNull(2) ?: StackTraceElement("", "", "", 0)
            return Pair(stackTraceElement.className.split('.').last().split('$').first(), stackTraceElement.methodName)
        }

        fun f(message: String = "", group: LoggingGroups = ALWAYS, thread: Pair<String, String> = callingMethod()){
            d("called", group = group, thread = thread)
        }

        fun d(message: String, group: LoggingGroups = ALWAYS, thread: Pair<String, String> = callingMethod()) {

            when(group) {
                ALWAYS -> {}
                FIRST_ONLY -> {
                    if(thread.second in methodsOnceOnly) // Method has been logged before
                        return

                    methodsOnceOnly.add(thread.second)
                }
                EVERY_TENTH -> {
                    val count = methodsEveryTenth.getOrDefault(thread.second, 0)

                    if (count % 10 == 0) { // Log on every 10th instance
                        methodsEveryTenth[thread.second] = 0

                    }
                    else { // Method was called too recently - skips logging for instances 1..9
                        methodsEveryTenth[thread.second] = count + 1
                        return
                    }
                }
                IF_TIME_ELAPSED_10S -> {
                    val lastCallTime = methods10s.getOrDefault(thread.second, 0L)
                    val currentTime = System.currentTimeMillis()

                    if (currentTime - lastCallTime < 10000) // Method was called too recently
                        return

                    synchronized(lock) {
                        methods10s[thread.second] = currentTime
                    }
                }
                IF_TIME_ELAPSED_1M -> {
                    val lastCallTime = methods1m.getOrDefault(thread.second, 0L)
                    val currentTime = System.currentTimeMillis()

                    if (currentTime - lastCallTime < 60000) // Method was called too recently
                        return
                    synchronized(lock) {
                        methods1m[thread.second] = currentTime
                    }
                }
            }

            Log.d(thread.first, "${thread.second} | $message")
        }

        fun w(message: String, thread: Pair<String, String> = callingMethod()) {
            Log.w(
                thread.first,
                "${thread.second} | $message"
            )
        }

        fun e(message: String, thread: Pair<String, String> = callingMethod()) {
            Log.e(
                thread.first,
                "${thread.second} | $message"
            )
        }

        fun i(message: String, thread: Pair<String, String> = callingMethod()) {
            Log.i(
                thread.first,
                "${thread.second} | $message"
            )
        }
    }
}