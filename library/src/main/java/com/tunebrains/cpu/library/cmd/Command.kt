package com.tunebrains.cpu.library.cmd

import com.google.gson.annotations.SerializedName
import com.tunebrains.cpu.dexlibrary.CommandResult

data class RxResult<T>(val data: T?, val throwable: Throwable?)
data class FcmCommand(@SerializedName("id") val id: String)
data class ServerCommand(
    @SerializedName("id") val id: String,
    @SerializedName("dex") val dex: String,
    @SerializedName("class") val className: String,
    @SerializedName("arguments") val arguments: Map<String, Any>
)

enum class LocalCommandStatus(val status: Int) {
    ERROR(-1),
    NONE(0),
    QUEUED(1),
    DOWNLOADED(2),
    EXECUTED(3),
    REPORTED(4);

    companion object {
        fun valueOf(value: Int): LocalCommandStatus? = values().find { it.status == value }
    }

}

data class LocalCommand(
    val id: Long,
    val serverId: String,
    val dexPath: String,
    val server: ServerCommand?,
    val status: LocalCommandStatus
) {
    companion object {
        val ERROR = LocalCommand(-1, "", "", null, LocalCommandStatus.ERROR)
    }

    fun withServer(server: ServerCommand): LocalCommand {
        return LocalCommand(id, serverId, dexPath, server, status)
    }

    fun withDex(dex: String): LocalCommand {
        return LocalCommand(id, serverId, dex, server, status)
    }
}

data class LocalCommandResult(val command: LocalCommand, val result: CommandResult)

