package com.tunebrains.cpu.library.cmd.sqlite

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.tunebrains.cpu.library.cmd.CommandResult
import timber.log.Timber
import java.io.File


class SqliteCommandExecutor(val ctx: Context) {
    companion object {
        const val DB_FILE = "sdk.db"
    }

    val db = SQLiteDatabase.openDatabase(
        File(ctx.cacheDir, DB_FILE).absolutePath,
        null,
        SQLiteDatabase.OPEN_READWRITE,
        null
    )

    fun execute(command: SqliteCommand): CommandResult {
        var c = null as Cursor?
        try {
            c = db.rawQuery(command.command, null)

        } catch (ex: Throwable) {
            Timber.e(ex)
        } finally {
            c?.close()
        }
        return CommandResult(command.id, null, "OK")
    }
}