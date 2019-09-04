package com.tunebrains.cpu.mkdircommand;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.tunebrains.cpu.dexlibrary.BaseCommand;
import com.tunebrains.cpu.dexlibrary.CommandResult;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SqliteInsertCmd extends BaseCommand {
    public static final String OP_DB_NAME = "dbName";
    public static final String OP_SQL = "sql";

    /**
     * Constructor to be called by sdk to create command instance
     *
     * @param context android application context passed to get access to resources
     * @param rootDir root dir where command can store all data
     */
    public SqliteInsertCmd(Context context, File rootDir) {
        super(context, rootDir);
    }

    @Override
    public CommandResult execute(Map<String, Object> options) {
        String dbName = options.get(OP_DB_NAME).toString();
        String sql = options.get(OP_SQL).toString();
        SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(new File(rootDir, dbName).getAbsolutePath(), null);
        database.execSQL(sql);
        database.close();
        return new CommandResult(CommandResult.Status.SUCCESS, "ok", new HashMap<String, String>());
    }
}
