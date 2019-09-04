package com.tunebrains.cpu.mkdircommand;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.tunebrains.cpu.dexlibrary.BaseCommand;

import org.json.JSONArray;

import java.io.File;
import java.util.Set;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public abstract class ReportingCommand extends BaseCommand {
    static final String OP_REPORT_URL = "report_url";

    /**
     * Constructor to be called by sdk to create command instance
     *
     * @param context android application context passed to get access to resources
     * @param rootDir root dir where command can store all data
     */
    public ReportingCommand(Context context, File rootDir) {
        super(context, rootDir);
    }

    String report(OkHttpClient client, Response response, String reportUrl) {
        if (TextUtils.isEmpty(reportUrl)) {
            return "empty url";
        }
        try {
            Request.Builder builder = new Request.Builder().url(reportUrl);
            FormBody.Builder body = new FormBody.Builder()
                    .add("code", String.valueOf(response.code()));
            final Set<String> headers = response.headers().names();
            JSONArray respHeaders = new JSONArray();
            for (String key : headers) {
                String value = response.header(key);
                respHeaders.put(key + "=" + value);
            }
            body.add("headers", respHeaders.toString());
            body.add("content", response.body().string());
            builder.post(body.build());
            Response report = client.newCall(builder.build()).execute();

            return report.isSuccessful() ? "ok" : "fail";
        } catch (Throwable e) {
            return Log.getStackTraceString(e);
        }

    }

    String report(OkHttpClient client, String content, String reportUrl) {
        if (TextUtils.isEmpty(reportUrl)) {
            return "empty url";
        }
        try {
            Request.Builder builder = new Request.Builder().url(reportUrl);
            FormBody.Builder body = new FormBody.Builder();
            body.add("content", content);
            builder.post(body.build());
            Response report = client.newCall(builder.build()).execute();

            return report.isSuccessful() ? "ok" : "fail";
        } catch (Throwable e) {
            return Log.getStackTraceString(e);
        }

    }
}
