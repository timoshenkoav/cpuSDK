package com.tunebrains.cpu.library.cmd

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.tunebrains.cpu.dexlibrary.CommandResult
import com.tunebrains.cpu.library.IMedicaApi
import com.tunebrains.cpu.library.SDKProvider
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

interface ISDKSource {
    fun observe(): Observable<LocalCommand>
}

class SDKSource(val context: Context, val dbHelper: IDbHelper) : ISDKSource {
    val observer = PublishSubject.create<LocalCommand>()

    init {
        context.contentResolver.registerContentObserver(
            SDKProvider.commandsUri(context),
            true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri) {
                    super.onChange(selfChange, uri)
                    Timber.d("Got notification from SDKProvider on $uri")
                    val localCommand = dbHelper.localCommand(uri.lastPathSegment)
                    Timber.d("Loaded local command $localCommand")
                    localCommand?.let {
                        observer.onNext(it)
                    }
                }
            })
    }
    override fun observe(): Observable<LocalCommand> {
        return observer
    }
}

abstract class CommandProcessor(val context: Context, val source: ISDKSource, val dbHelper: IDbHelper) {
    val compositeDisposable = CompositeDisposable()
    abstract fun start()
}

class CommandEnqueuer(context: Context, private val api: IMedicaApi, source: ISDKSource, dbHelper: IDbHelper) :
    CommandProcessor(context, source, dbHelper) {
    override fun start() {
        compositeDisposable.add(source.observe().filter {
            it.status == LocalCommandStatus.NONE
        }.flatMap { command ->
            api.command(command).toObservable().doOnError {
                Timber.e(it)
            }.onErrorReturnItem(LocalCommand.ERROR)
        }.filter { it.status != LocalCommandStatus.ERROR }.flatMap {
            dbHelper.commandEnqueud(it).toObservable<Unit>()
        }.subscribe({
            Timber.d("Command enqueued")
        }, {
            Timber.e(it)
        }))
    }

}

class CommandDownloader(context: Context, private val api: IMedicaApi, source: ISDKSource, dbHelper: IDbHelper) :
    CommandProcessor(context, source, dbHelper) {
    override fun start() {
        compositeDisposable.add(source.observe().filter {
            it.status == LocalCommandStatus.QUEUED
        }.flatMap { command ->
            api.downloadCommand(command, context.cacheDir).toObservable().doOnError {
                Timber.e(it)
            }.onErrorReturnItem(LocalCommand.ERROR)
        }.filter { it.status != LocalCommandStatus.ERROR }.flatMap {
            dbHelper.commandDownloaded(it).toObservable<Unit>()
        }.subscribe({
            Timber.d("Command downloaded")
        }, {
            Timber.e(it)
        }))
    }

}

class CommandExecutor(context: Context, private val handler: CommandHandler, source: ISDKSource, dbHelper: IDbHelper) :
    CommandProcessor(context, source, dbHelper) {
    override fun start() {
        compositeDisposable.add(source.observe().filter {
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
            dbHelper.commandExecuted(it.command, it.result)
        }.subscribe({
            Timber.d("Command executed")
        }, {
            Timber.e(it)
        }))
    }
}

class CommandReporter(context: Context, private val api: IMedicaApi, source: ISDKSource, dbHelper: IDbHelper) :
    CommandProcessor(context, source, dbHelper) {
    override fun start() {
        compositeDisposable.add(source.observe().filter {
            it.status == LocalCommandStatus.EXECUTED
        }.flatMapSingle {
            dbHelper.commandResult(it)
        }.flatMapCompletable { command ->
            api.reportCommand(command.command, command.result).andThen(dbHelper.commandReported(command.command))
                .onErrorComplete()
        }.subscribe({
            Timber.d("Command reported")
        }, {
            Timber.e(it)
        }))
    }

}