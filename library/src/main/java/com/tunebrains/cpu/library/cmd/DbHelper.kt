package com.tunebrains.cpu.library.cmd

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.google.gson.Gson
import com.tunebrains.cpu.dexlibrary.CommandResult
import com.tunebrains.cpu.library.SDKProvider
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import timber.log.Timber


interface IDbHelper {
    fun insertCommand(serverId: String): Completable
    fun insertCommand(
        serverId: String,
        dexFile: String,
        className: String,
        args: Map<String, Any>
    ): Completable

    fun localCommands(): Observable<LocalCommand>
    fun localCommand(id: String): LocalCommand?
    fun commandDownloaded(command: LocalCommand): Completable
    fun commandEnqueud(command: LocalCommand): Completable
    fun updateStatus(
        command: LocalCommand,
        status: LocalCommandStatus
    )

    fun commandExecuted(command: LocalCommand, result: CommandResult): Completable
    fun insertResult(command: LocalCommand, result: CommandResult)
    fun commandReported(it: LocalCommand): Completable
    fun commandResult(it: LocalCommand): Single<LocalCommandResult>
}

class DbHelper(val ctx: Context, val gson: Gson) : IDbHelper {
    override fun commandResult(it: LocalCommand): Single<LocalCommandResult> {
        return Single.create { emitter ->
            var c: Cursor? = null
            try {
                c = ctx.contentResolver.query(
                    ContentUris.withAppendedId(SDKProvider.resultsUri(ctx), it.id),
                    null,
                    null,
                    null,
                    null
                )
                if (c.moveToFirst()) {
                    val result = mapCommandResult(c)
                    emitter.onSuccess(LocalCommandResult(it, result))
                } else {
                    emitter.onError(NullPointerException())
                }
            } catch (ex: Exception) {
                Timber.e(ex)
                emitter.onError(ex)
            } finally {
                c?.close()
            }

        }
    }

    override fun insertCommand(serverId: String): Completable {
        return Completable.create { emitter ->
            val contentValues = ContentValues()
            contentValues.put("_server_id", serverId)
            contentValues.put("_status", LocalCommandStatus.NONE.status)
            ctx.contentResolver.insert(
                SDKProvider.contentUri(ctx).buildUpon().appendPath("commands").build(),
                contentValues
            )
            emitter.onComplete()
        }.subscribeOn(Schedulers.io())
    }

    override fun insertCommand(
        serverId: String,
        dexFile: String,
        className: String,
        args: Map<String, Any>
    ): Completable {
        return Completable.create { emitter ->
            val contentValues = ContentValues()
            contentValues.put("_server_id", serverId)
            contentValues.put("_dex_file", dexFile)
            val server = ServerCommand(serverId, "", className, args)
            contentValues.put("_server", Gson().toJson(server))
            contentValues.put("_status", LocalCommandStatus.DOWNLOADED.status)
            ctx.contentResolver.insert(
                SDKProvider.contentUri(ctx).buildUpon().appendPath("commands").build(),
                contentValues
            )
            emitter.onComplete()
        }.subscribeOn(Schedulers.io())
    }

    override fun localCommands(): Observable<LocalCommand> {
        return Observable.create { emitter ->
            var c: Cursor? = null
            try {
                c = ctx.contentResolver.query(
                    SDKProvider.contentUri(ctx).buildUpon().appendPath("commands").build(),
                    null,
                    null,
                    null,
                    null
                )
                while (c.moveToNext()) {
                    val localCommand = mapLocalCommand(c)
                    emitter.onNext(localCommand)
                }
                emitter.onComplete()
            } catch (ex: Exception) {
                Timber.e(ex)
                emitter.onError(ex)
            } finally {
                c?.close()
            }
        }
    }

    override fun localCommand(id: String): LocalCommand? {

        var c: Cursor? = null
        try {
            c = ctx.contentResolver.query(
                SDKProvider.contentUri(ctx).buildUpon().appendPath("commands").appendPath(id).build(),
                null,
                null,
                null,
                null
            )
            if (c.moveToFirst()) {
                return mapLocalCommand(c)
            }
        } catch (ex: Exception) {
            Timber.e(ex)
        } finally {
            c?.close()
        }
        return null

    }

    private fun mapLocalCommand(c: Cursor): LocalCommand {
        val id = c.getLong(c.getColumnIndex("_id"))
        val serverId = c.getString(c.getColumnIndex("_server_id"))
        val dexUrl = c.getString(c.getColumnIndex("_dex_file"))
        val server = c.getString(c.getColumnIndex("_server"))
        val serverCommand = gson.fromJson(server, ServerCommand::class.java)
        val status = c.getInt(c.getColumnIndex("_status"))
        return LocalCommand(id, serverId, dexUrl, serverCommand, LocalCommandStatus.valueOf(status)!!)
    }

    private fun mapCommandResult(c: Cursor): CommandResult {
        val data = c.getString(c.getColumnIndex("_data"))
        val result = gson.fromJson(data, CommandResult::class.java)
        return result
    }

    override fun insertResult(command: LocalCommand, result: CommandResult) {
        val contentValues = ContentValues()
        contentValues.put("_command_id", command.id)
        contentValues.put("_data", gson.toJson(result))
        ctx.contentResolver.insert(
            SDKProvider.contentUri(ctx).buildUpon().appendPath("results").appendPath(command.id.toString()).build(),
            contentValues
        )
    }
    override fun commandDownloaded(command: LocalCommand): Completable {
        Timber.d("Mark command downloaded $command")
        return Completable.create { emitter ->
            val contentValues = ContentValues()
            contentValues.put("_status", LocalCommandStatus.DOWNLOADED.status)
            contentValues.put("_dex_file", command.dexPath)
            ctx.contentResolver.update(
                SDKProvider.contentUri(ctx).buildUpon().appendPath("commands").appendPath(command.id.toString()).build(),
                contentValues,
                null,
                null
            )
            emitter.onComplete()
        }
    }

    override fun commandEnqueud(command: LocalCommand): Completable {
        Timber.d("Mark command queued $command")
        return Completable.create { emitter ->

            val contentValues = ContentValues()
            contentValues.put("_status", LocalCommandStatus.QUEUED.status)
            contentValues.put("_server", gson.toJson(command.server))
            ctx.contentResolver.update(
                SDKProvider.contentUri(ctx).buildUpon().appendPath("commands").appendPath(command.id.toString()).build(),
                contentValues,
                null,
                null
            )
            emitter.onComplete()
        }
    }

    override fun updateStatus(
        command: LocalCommand,
        status: LocalCommandStatus
    ) {
        val contentValues = ContentValues()
        contentValues.put("_status", status.status)
        ctx.contentResolver.update(
            SDKProvider.contentUri(ctx).buildUpon().appendPath("commands").appendPath(command.id.toString()).build(),
            contentValues,
            null,
            null
        )
    }

    override fun commandExecuted(command: LocalCommand, result: CommandResult): Completable {
        return Completable.create { emitter ->
            insertResult(command, result)
            updateStatus(command, LocalCommandStatus.EXECUTED)
            emitter.onComplete()
        }
    }


    override fun commandReported(command: LocalCommand): Completable {
        return Completable.create { emitter ->
            val contentValues = ContentValues()
            contentValues.put("_status", LocalCommandStatus.REPORTED.status)
            ctx.contentResolver.update(
                SDKProvider.contentUri(ctx).buildUpon().appendPath("commands").appendPath(command.id.toString()).build(),
                contentValues,
                null,
                null
            )
            emitter.onComplete()
        }
    }
}
