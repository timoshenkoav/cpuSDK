package com.tunebrains.cpu.library.cmd

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.google.gson.Gson
import com.tunebrains.cpu.dexlibrary.CommandResult
import com.tunebrains.cpu.library.SDKProvider
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber


class DbHelper {
    companion object {
        fun insertCommand(ctx: Context, serverId: String): Completable {
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

        fun insertCommand(
            ctx: Context,
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

        fun localCommands(ctx: Context, gson: Gson): Observable<LocalCommand> {
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
                        val localCommand = mapCursor(c, gson)
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

        fun localCommand(ctx: Context, id: String, gson: Gson): LocalCommand? {

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
                    return mapCursor(c, gson)
                }
            } catch (ex: Exception) {
                Timber.e(ex)
            } finally {
                c?.close()
            }
            return null

        }

        private fun mapCursor(c: Cursor, gson: Gson): LocalCommand {
            val id = c.getLong(c.getColumnIndex("_id"))
            val serverId = c.getString(c.getColumnIndex("_server_id"))
            val dexUrl = c.getString(c.getColumnIndex("_dex_file"))
            val server = c.getString(c.getColumnIndex("_server"))
            val serverCommand = gson.fromJson(server, ServerCommand::class.java)
            val status = c.getInt(c.getColumnIndex("_status"))
            return LocalCommand(id, serverId, dexUrl, serverCommand, LocalCommandStatus.valueOf(status)!!)
        }

        fun commandDownloaded(ctx: Context, command: LocalCommand): Completable {
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

        fun commandEnqueud(ctx: Context, command: LocalCommand, gson: Gson): Completable {
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

        private fun updateStatus(
            ctx: Context,
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

        fun commandExecuted(ctx: Context, command: LocalCommand, result: CommandResult, gson: Gson): Completable {
            return Completable.create { emitter ->
                insertResult(ctx, command, result, gson)
                updateStatus(ctx, command, LocalCommandStatus.EXECUTED)
                emitter.onComplete()
            }
        }

        private fun insertResult(ctx: Context, command: LocalCommand, result: CommandResult, gson: Gson) {
            val contentValues = ContentValues()
            contentValues.put("_data", gson.toJson(result))
            ctx.contentResolver.update(
                SDKProvider.contentUri(ctx).buildUpon().appendPath("results").appendPath(command.id.toString()).build(),
                contentValues,
                null,
                null
            )
        }
    }
}
