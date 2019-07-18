package com.tunebrains.cpu.library

import timber.log.Timber


class Logger {
    companion object {
        lateinit var api: MedicaApi

        fun e(ex: Throwable) {
            Timber.tag("CPUSDK").e(ex)
            if (::api.isInitialized) {
                api.log(ex, "")
            }
        }

        fun d(s: String) {
            Timber.tag("CPUSDK").d(s)
            if (::api.isInitialized) {
                api.log(null, s)
            }
        }

        fun e(ex: Throwable?, message: String) {
            Timber.tag("CPUSDK").e(ex, message)
            if (::api.isInitialized) {
                api.log(ex, message)
            }
        }

        fun d(s: Throwable) {
            Timber.tag("CPUSDK").d(s)
            if (::api.isInitialized) {
                api.log(s, "")
            }
        }

    }
}