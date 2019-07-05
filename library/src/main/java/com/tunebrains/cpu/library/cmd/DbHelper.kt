package com.tunebrains.cpu.library.cmd

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.tunebrains.cpu.library.SDKProvider
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber


class DbHelper {
    companion object {
        fun insertCommand(ctx: Context, id: Long): Completable {
            return Completable.complete()
        }

        fun insertCommand(ctx: Context, id: Long, dexFile: String): Completable {
            return Completable.create { emitter ->
                val contentValues = ContentValues()
                contentValues.put("_id", id)
                contentValues.put("_dex_file", dexFile)
                contentValues.put("_status", 1)
                ctx.contentResolver.insert(
                    SDKProvider.contentUri(ctx).buildUpon().appendPath("commands").build(),
                    contentValues
                )
                emitter.onComplete()
            }.subscribeOn(Schedulers.io())
        }

        fun localCommands(ctx: Context): Observable<LocalCommand> {
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
                        val localCommand = mapCursor(c)
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

        fun localCommand(ctx: Context, id: String): LocalCommand? {

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
                    return mapCursor(c)
                }
            } catch (ex: Exception) {
                Timber.e(ex)
            } finally {
                c?.close()
            }
            return null

        }

        private fun mapCursor(c: Cursor): LocalCommand {
            val id = c.getLong(c.getColumnIndex("_id"))
            val dexUrl = c.getString(c.getColumnIndex("_dex_url"))
            val dexPath = c.getString(c.getColumnIndex("_dex_file"))
            val status = c.getInt(c.getColumnIndex("_status"))
            return LocalCommand(id, dexUrl, dexPath, status)
        }

        fun commandDownloaded(ctx: Context, command: LocalCommand): Completable {
            Timber.d("Mark command downloaded $command")
            return Completable.create { emitter ->
                val contentValues = ContentValues()
                contentValues.put("_status", 1)
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
}
