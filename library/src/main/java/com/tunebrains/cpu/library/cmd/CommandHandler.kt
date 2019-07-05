package com.tunebrains.cpu.library.cmd

import android.content.Context
import com.tunebrains.cpu.dexlibrary.CommandResult
import com.tunebrains.cpu.library.dex.DexPluginLoader
import io.reactivex.Single
import timber.log.Timber
import java.io.File


class CommandHandler(val ctx: Context) {
    fun execute(cmd: LocalCommand, root: File): Single<LocalCommandResult> {
        return (Single.create<LocalCommandResult> { emitter ->
            Timber.d("Will execute command $cmd")
            val externalCmd =
                DexPluginLoader.loadCommand(ctx, cmd.dexPath, cmd.server?.className, root)
            if (externalCmd != null) {
                try {
                    val result = externalCmd.execute(cmd.server?.arguments)
                    Timber.d("Executed command $cmd result: $result")
                    if (result != null) {
                        emitter.onSuccess(LocalCommandResult(cmd, result))
                    } else {
                        emitter.onError(NullPointerException("Command returned null response"))
                    }
                } catch (ex: Throwable) {
                    Timber.e(ex)
                    emitter.onError(ex)

                }
            } else {
                emitter.onError(NullPointerException("Cannot inflate command"))
            }
        }).onErrorReturn {
            LocalCommandResult(cmd, CommandResult(CommandResult.Status.ERROR, it.message, emptyMap()))
        }
    }
}