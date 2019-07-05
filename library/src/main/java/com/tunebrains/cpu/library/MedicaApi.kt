package com.tunebrains.cpu.library

import com.google.gson.Gson
import com.tunebrains.cpu.library.cmd.LocalCommand
import com.tunebrains.cpu.library.cmd.ServerCommand
import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.*
import timber.log.Timber
import java.io.File
import java.io.IOException

data class IPIinfo(val ip: String)
class MedicaApi(val gson: Gson, val repository: TokenRepository) {
    private val client = OkHttpClient.Builder().build()
    fun ip(): Single<IPIinfo> {
        return Single.create<IPIinfo> { emitter ->
            client.newCall(Request.Builder().url("https://api6.ipify.org?format=json").build())
                .enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (!emitter.isDisposed) {
                            emitter.onError(e)
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: ""
                            val info = gson.fromJson<IPIinfo>(body, IPIinfo::class.java)
                            if (!emitter.isDisposed) {
                                emitter.onSuccess(info)
                            }
                        }
                    }

                })
        }.retry(3)
    }

    fun informServer(ip: String): Completable {
        return Completable.complete()
    }

    fun command(id: Long): Single<ServerCommand> {
        return Single.error(NullPointerException())
    }

    fun downloadFile(url: String, root: File): Single<File> {
        return Single.just(File(""))
    }

    fun downloadCommand(command: LocalCommand, cacheDir: File): Single<LocalCommand> {
        Timber.d("Will download command $command")
        return Single.just(command)
    }
}