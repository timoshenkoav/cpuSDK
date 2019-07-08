package com.tunebrains.cpu.sample

import android.app.Application
import android.util.Log
import org.jetbrains.annotations.NotNull
import timber.log.Timber
import java.io.File
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*


class App : Application() {
    lateinit var logFileTree: LogFileTree
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        logFileTree = LogFileTree(filesDir)
        Timber.plant(logFileTree)
    }

    class LogFileTree(cacheDir: File) : @NotNull Timber.DebugTree() {
        val logFile = File(cacheDir, "log-${SimpleDateFormat.getDateInstance().format(Date())}.log")
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            logFile.appendText("$tag - $message\n", Charset.defaultCharset())
            t?.let {
                logFile.appendText("\n${Log.getStackTraceString(it)}\n", Charset.defaultCharset())
            }
        }

    }
}