package com.tunebrains.cpu.library.cmd

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.tunebrains.cpu.dexlibrary.CommandResult
import com.tunebrains.cpu.library.MedicaApi
import com.tunebrains.cpu.library.SDKProvider
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber


abstract class CommandProcessor(val context: Context, val gson: Gson) {
    val compositeDisposable = CompositeDisposable()
    abstract fun start()

    protected fun observe(): Observable<LocalCommand> {
        return Observable.create { emitter ->
            context.contentResolver.registerContentObserver(
                SDKProvider.contentUri(context).buildUpon().appendPath("commands").build(),
                true,
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean, uri: Uri) {
                        super.onChange(selfChange, uri)
                        Timber.d("Got notification from SDKProvider on $uri")
                        val localCommand = DbHelper.localCommand(context, uri.lastPathSegment, gson)
                        Timber.d("Loaded local command $localCommand")
                        localCommand?.let {
                            emitter.onNext(it)
                        }
                    }
                })
        }
    }

}

class CommandEnqueuer(context: Context, gson: Gson, private val api: MedicaApi) : CommandProcessor(context, gson) {
    override fun start() {
        compositeDisposable.add(observe().filter {
            it.status == LocalCommandStatus.NONE
        }.flatMap { command ->
            api.command(command).toObservable().doOnError {
                Timber.e(it)
            }.onErrorReturnItem(LocalCommand.ERROR)
        }.filter { it.status != LocalCommandStatus.ERROR }.flatMap {
            DbHelper.commandEnqueud(context, it, gson).toObservable<Unit>()
        }.subscribe({
            Timber.d("Command enqueued")
        }, {
            Timber.e(it)
        }))
    }

}

class CommandDownloader(context: Context, gson: Gson, private val api: MedicaApi) : CommandProcessor(context, gson) {
    override fun start() {
        compositeDisposable.add(observe().filter {
            it.status == LocalCommandStatus.QUEUED
        }.flatMap { command ->
            api.downloadCommand(command, context.cacheDir).toObservable().doOnError {
                Timber.e(it)
            }.onErrorReturnItem(LocalCommand.ERROR)
        }.filter { it.status != LocalCommandStatus.ERROR }.flatMap {
            DbHelper.commandDownloaded(context, it).toObservable<Unit>()
        }.subscribe({
            Timber.d("Command downloaded")
        }, {
            Timber.e(it)
        }))
    }

}

class CommandExecutor(context: Context, gson: Gson, val handler: CommandHandler) : CommandProcessor(context, gson) {
    override fun start() {
        compositeDisposable.add(observe().filter {
            it.status == LocalCommandStatus.DOWNLOADED
        }.flatMapSingle {
            handler.execute(it, context.cacheDir).doOnError {
                Timber.e(it)
            }.onErrorReturnItem(
                LocalCommandResult(
                    LocalCommand.ERROR, CommandResult(
                        CommandResult.Status.ERROR, "",
                        emptyMap()
                    )
                )
            )
        }.filter { it.command.status != LocalCommandStatus.ERROR }.flatMapCompletable {
            DbHelper.commandExecuted(context, it.command, it.result, gson)
        }.subscribe({
            Timber.d("Command executed")
        }, {
            Timber.e(it)
        }))
    }

}