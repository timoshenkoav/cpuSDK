package com.tunebrains.cpu.library

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.google.gson.Gson
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber


class SDKProvider : ContentProvider() {
    lateinit var sdk: CPUSdk
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        return null
    }

    private val compositeDisposable = CompositeDisposable()
    override fun onCreate(): Boolean {
        val gson = Gson()
        val repository = TokenRepository(context!!)

        val api = MedicaApi(gson, repository)
        sdk = CPUSdk(context!!, api)
        sdk.init()

        val remoteCommand = RemoteCommandProvider()
        remoteCommand.start()

        compositeDisposable.add(remoteCommand.commandsObserver.subscribe { command ->
            compositeDisposable.add(api.command(command).subscribe({

            }, {
                Timber.e(it)
            }))
        })
        return true
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    override fun getType(uri: Uri): String? {
        return null
    }
}