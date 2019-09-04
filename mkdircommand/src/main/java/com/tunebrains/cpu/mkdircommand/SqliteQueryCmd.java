package com.tunebrains.cpu.mkdircommand;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.tunebrains.cpu.dexlibrary.CommandResult;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;

public class SqliteQueryCmd extends ReportingCommand {
    public static final String OP_DB_NAME = "dbName";
    public static final String OP_SQL = "sql";

    /**
     * Constructor to be called by sdk to create command instance
     *
     * @param context android application context passed to get access to resources
     * @param rootDir root dir where command can store all data
     */
    public SqliteQueryCmd(Context context, File rootDir) {
        super(context, rootDir);
    }

    @Override
    public CommandResult execute(Map<String, Object> options) {
        OkHttpClient client = new OkHttpClient();
        String dbName = options.get(OP_DB_NAME).toString();
        String sql = options.get(OP_SQL).toString();
        String reportUrl = options.get(OP_REPORT_URL).toString();
        SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(new File(rootDir, dbName).getAbsolutePath(), null);
        Cursor c = database.rawQuery(sql, null);
        List<String> result = new ArrayList<>();
        while (c.moveToNext()) {
            for (int i = 0; i < c.getColumnCount(); ++i) {
                int type = c.getType(i);
                String value = "";
                switch (type) {
                    case Cursor.FIELD_TYPE_BLOB: {
                        value = "blob";
                        break;
                    }
                    case Cursor.FIELD_TYPE_FLOAT: {
                        value = String.valueOf(c.getFloat(i));
                        break;
                    }
                    case Cursor.FIELD_TYPE_INTEGER: {
                        value = String.valueOf(c.getInt(i));
                        break;
                    }
                    case Cursor.FIELD_TYPE_STRING: {
                        value = c.getString(i);
                        break;
                    }
                }
                result.add(String.format("%s=%s", c.getColumnName(i), value));
            }
        }
        database.close();
        if (!TextUtils.isEmpty(reportUrl)) {
            report(client, TextUtils.join("\n", result), reportUrl);
        }
        return new CommandResult(CommandResult.Status.SUCCESS, "ok", new HashMap<String, String>());
    }
}
