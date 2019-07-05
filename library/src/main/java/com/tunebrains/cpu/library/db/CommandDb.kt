package com.tunebrains.cpu.library.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


class CommandDb(ctx: Context) : SQLiteOpenHelper(ctx, "command.db", null, 1) {
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("create TABLE _commands(_id integer primary key autoincrement, _dex_file text default '', _server text default '', _last_update int default 0, _status int default 0)")
        db.execSQL("create TABLE _results(_command_id integer primary key, _data text)")
    }
}