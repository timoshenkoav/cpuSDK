package com.tunebrains.cpu.library

import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.*
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
}