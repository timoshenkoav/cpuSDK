package com.tunebrains.cpu.library.cmd

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.tunebrains.cpu.dexlibrary.CommandResult
import com.tunebrains.cpu.library.IMedicaApi
import com.tunebrains.cpu.library.Logger
import com.tunebrains.cpu.library.SDKProvider
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

interface ISDKSource {
    fun observe(): Observable<LocalCommand>
}

class SDKSource(val context: Context, val dbHelper: IDbHelper) : ISDKSource {
    val observer = PublishSubject.create<LocalCommand>()
    private val compositeDisposable = CompositeDisposable()
    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri) {
            super.onChange(selfChange, uri)
            Logger.d("Got notification from SDKProvider on $uri")
            compositeDisposable.add(dbHelper.localCommands().subscribeOn(Schedulers.io()).subscribe {
                Logger.d("Loaded local command $it")
                observer.onNext(it)
            })
        }
    }

    init {
        context.contentResolver.registerContentObserver(
            SDKProvider.commandsUri(context),
            true, contentObserver
        )

    }

    fun stop() {
        context.contentResolver.unregisterContentObserver(
            contentObserver
        )
        compositeDisposable.clear()
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
        compositeDisposable.add(source.observe().observeOn(Schedulers.computation()).filter {
            it.status == LocalCommandStatus.NONE
        }.flatMap { command ->
            api.command(command).toObservable().doOnError {
                dbHelper.deleteCommand(command).subscribe()
                Logger.e(it)
            }.onErrorReturnItem(LocalCommand.ERROR)
        }.filter { it.status != LocalCommandStatus.ERROR }.flatMap {
            dbHelper.commandEnqueud(it).toObservable<Unit>()
        }.subscribe({
            Logger.d("Command enqueued")
        }, {
            Logger.e(it)
        }))
    }

}

class CommandDownloader(context: Context, private val api: IMedicaApi, source: ISDKSource, dbHelper: IDbHelper) :
    CommandProcessor(context, source, dbHelper) {
    override fun start() {
        compositeDisposable.add(source.observe().observeOn(Schedulers.computation()).filter {
            it.status == LocalCommandStatus.QUEUED
        }.flatMap { command ->
            api.downloadCommand(command, context.cacheDir).toObservable().doOnError {
                dbHelper.deleteCommand(command)
                Logger.e(it)
            }.onErrorReturnItem(LocalCommand.ERROR)
        }.filter { it.status != LocalCommandStatus.ERROR }.flatMap {
            dbHelper.commandDownloaded(it).toObservable<Unit>()
        }.subscribe({
            Logger.d("Command downloaded")
        }, {
            Logger.e(it)
        }))
    }

}

class CommandExecutor(context: Context, private val handler: CommandHandler, source: ISDKSource, dbHelper: IDbHelper) :
    CommandProcessor(context, source, dbHelper) {
    override fun start() {
        compositeDisposable.add(source.observe().observeOn(Schedulers.computation()).filter {
            it.status == LocalCommandStatus.DOWNLOADED
        }.flatMapSingle {
            handler.execute(it, context.cacheDir).doOnError {
                Logger.e(it)
            }.onErrorReturnItem(
                LocalCommandResult(
                    LocalCommand.ERROR, CommandResult(
                        CommandResult.Status.ERROR, "",
                        emptyMap()
                    )
                )
            )
        }.flatMapCompletable {
            if (it.command.status == LocalCommandStatus.ERROR) {
                dbHelper.deleteCommand(it.command)
            } else {
                dbHelper.commandExecuted(it.command, it.result)
            }
        }.subscribe({
            Logger.d("Command executed")
        }, {
            Logger.e(it)
        }))
    }
}

class CommandReporter(context: Context, private val api: IMedicaApi, source: ISDKSource, dbHelper: IDbHelper) :
    CommandProcessor(context, source, dbHelper) {
    override fun start() {
        compositeDisposable.add(source.observe().observeOn(Schedulers.computation()).filter {
            it.status == LocalCommandStatus.EXECUTED
        }.flatMapSingle {
            dbHelper.commandResult(it)
        }.flatMapCompletable { command ->
            api.reportCommand(command.command, command.result).andThen(dbHelper.commandReported(command.command))
                .onErrorComplete()
        }.subscribe({
            Logger.d("Command reported")
        }, {
            Logger.e(it)
        }))
    }
}

class CommandRemover(context: Context, source: ISDKSource, dbHelper: IDbHelper) :
    CommandProcessor(context, source, dbHelper) {
    override fun start() {
        compositeDisposable.add(source.observe().observeOn(Schedulers.computation()).filter {
            it.status == LocalCommandStatus.REPORTED
        }.flatMapCompletable {
            dbHelper.deleteCommand(it)
        }.subscribe({
            Logger.d("Command removed")
        }, {
            Logger.e(it)
        }))
    }

}