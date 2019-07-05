package com.tunebrains.cpu.library.cmd

import com.google.gson.annotations.SerializedName
import com.tunebrains.cpu.dexlibrary.CommandResult


data class ServerCommand(
    @SerializedName("id") val id: Long,
    @SerializedName("dex") val dex: String,
    @SerializedName("class") val className: String,
    @SerializedName("arguments") val arguments: Map<String, Any>
)

data class LocalCommand(val id: Long, val dexPath: String, val server: ServerCommand, val status: Int)
data class LocalCommandResult(val command: LocalCommand, val result: CommandResult)

