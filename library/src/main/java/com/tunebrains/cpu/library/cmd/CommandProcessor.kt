package com.tunebrains.cpu.library.cmd

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import com.tunebrains.cpu.library.MedicaApi
import com.tunebrains.cpu.library.SDKProvider
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber


abstract class CommandProcessor(val context: Context) {
    val compositeDisposable = CompositeDisposable()
    abstract fun start()

    protected fun observe(): Observable<LocalCommand> {
        return (Observable.create<LocalCommand> { emitter ->
            context.contentResolver.registerContentObserver(
                SDKProvider.contentUri(context).buildUpon().appendPath("commands").build(),
                true,
                object : ContentObserver(Handler()) {
                    override fun onChange(selfChange: Boolean, uri: Uri) {
                        super.onChange(selfChange, uri)
                        Timber.d("Got notification from SDKProvider on $uri")
                        val localCommand = DbHelper.localCommand(context, uri.lastPathSegment)
                        Timber.d("Loaded local command $localCommand")
                        localCommand?.let {
                            emitter.onNext(it)
                        }
                    }
                })
        })
    }

}

class CommandDownloader(context: Context, val api: MedicaApi) : CommandProcessor(context) {
    override fun start() {
        compositeDisposable.add(observe().filter {
            it.status == 0
        }.flatMapSingle { command ->
            api.downloadCommand(command, context.cacheDir)
        }.flatMapCompletable {
            DbHelper.commandDownloaded(context, it)
        }.subscribe({
            Timber.d("Command downloaded")
        }, {
            Timber.e(it)
        }))
    }

}