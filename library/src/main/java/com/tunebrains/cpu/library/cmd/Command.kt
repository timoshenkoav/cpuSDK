package com.tunebrains.cpu.library.cmd


data class ServerCommand(val id: Long, val dexUrl: String)
data class LocalCommand(val id: Long, val dexUrl: String, val dexPath: String, val status: Int)

