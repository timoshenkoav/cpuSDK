package com.tunebrains.cpu.library

import android.content.*
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.brobwind.bronil.ProxyServer
import com.google.gson.Gson
import com.tunebrains.cpu.library.cmd.*
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

        fun commandsUri(ctx: Context): Uri {
            return Uri.parse("content://${authority(ctx)}/commands")
        }

        fun resultsUri(ctx: Context): Uri {
            return Uri.parse("content://${authority(ctx)}/results")
        }

        fun fcmUri(ctx: Context): Uri {
            return Uri.parse("content://${authority(ctx)}/fcm")
        }

        fun fcmDataUri(ctx: Context): Uri {
            return Uri.parse("content://${authority(ctx)}/fcm/data")
        }
    }

    val sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)


    private val compositeDisposable = CompositeDisposable()
    private lateinit var db: CommandDb
    private lateinit var tokenRepository: TokenRepository
    private lateinit var dbHelper: DbHelper
    private lateinit var gson: Gson
    override fun onCreate(): Boolean {
        sUriMatcher.apply {
            addURI(authority(context!!), "commands", 1)
            addURI(authority(context!!), "commands/#", 2)
            addURI(authority(context!!), "results", 3)
            addURI(authority(context!!), "results/#", 4)
            addURI(authority(context!!), "fcm", 5)
            addURI(authority(context!!), "fcm/data", 6)
        }
        gson = Gson()
        tokenRepository = TokenRepository(context!!)
        db = CommandDb(context!!)
        val api = MedicaApi(gson, tokenRepository)

        sdk = CPUSdk(context!!, api, tokenRepository)
        sdk.init()

        dbHelper = DbHelper(context!!, gson)
        val source = SDKSource(context!!, dbHelper)

        val remoteCommand = RemoteCommandProvider()
        remoteCommand.start()

        val commandDownloader = CommandDownloader(context!!, api, source, dbHelper)
        commandDownloader.start()

        val commandExecutor = CommandExecutor(context!!, CommandHandler(context!!), source, dbHelper)
        commandExecutor.start()

        val commandEnqueuer = CommandEnqueuer(context!!, api, source, dbHelper)
        commandEnqueuer.start()

        val commandReporter = CommandReporter(context!!, api, source, dbHelper)
        commandReporter.start()

        val commandRemover = CommandRemover(context!!, source, dbHelper)
        commandRemover.start()

        compositeDisposable.add(remoteCommand.commandsObserver.subscribe { command ->
            dbHelper.insertCommand(command)
        })

        val proxy = ProxyServer(9877)
        proxy.startServer()

        return true
    }

    override fun insert(uri: Uri, values: ContentValues): Uri? {
        return when (sUriMatcher.match(uri)) {
            1 -> {
                val id =
                    db.writableDatabase.insertWithOnConflict("_commands", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                val url = ContentUris.withAppendedId(commandsUri(context!!), id)
                context!!.contentResolver.notifyChange(url, null)
                return url
            }
            4 -> {
                val id =
                    db.writableDatabase.insertWithOnConflict("_results", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                context!!.contentResolver.notifyChange(uri, null)
                return uri
            }
            5 -> {
                val token = values.getAsString("token")
                if (!token.isNullOrBlank()) {
                    tokenRepository.saveFcmToken(token)
                }
                return uri
            }
            6 -> {
                val token = values.getAsString("data")
                if (!token.isNullOrBlank()) {
                    val fcmCommand = gson.fromJson(token, FcmCommand::class.java)
                    if (fcmCommand != null && !fcmCommand.id.isNullOrBlank()) {
                        dbHelper.insertCommand(fcmCommand.id).subscribe({
                            Timber.d("Fcm Command $fcmCommand inserted")
                        }, {
                            Timber.e(it)
                        })
                    } else {
                        Timber.d("Got ping with FCM")
                        sdk.ping()
                    }
                }
                return uri
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
            4 -> {
                db.readableDatabase.query(
                    "_results",
                    projection,
                    "_command_id=?",
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
                val ret = db.writableDatabase.update("_commands", values, "_id=?", arrayOf(uri.lastPathSegment))
                context!!.contentResolver.notifyChange(uri, null)
                ret
            }
            else ->
                0
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return when (sUriMatcher.match(uri)) {
            2 -> {
                db.writableDatabase.delete(
                    "_commands",
                    "_id=?",
                    arrayOf(uri.lastPathSegment)
                )
            }
            4 -> {
                db.writableDatabase.delete(
                    "_results",
                    "_command_id=?",
                    arrayOf(uri.lastPathSegment)
                )
            }
            else ->
                0
        }

    }

    override fun getType(uri: Uri): String? {
        return null
    }
}