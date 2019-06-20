package com.tunebrains.cpu.library.cmd


interface CommandProcessor {
    fun enqueueCommand(cmd: Command)
}