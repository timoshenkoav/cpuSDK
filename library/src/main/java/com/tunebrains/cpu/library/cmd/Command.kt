package com.tunebrains.cpu.library.cmd

data class CommandResult(val id: Long, val ex: Throwable?, val data: String?)

interface Command

abstract class BaseCommand(val id: Long) : Command

