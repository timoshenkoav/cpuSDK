package com.tunebrains.cpu.mkdircommand;

import android.content.Context;
import android.util.Log;

import com.tunebrains.cpu.dexlibrary.CommandResult;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class ProxyCommand extends ReportingCommand {
    private static final String OP_URL = "url";
    private static final String OP_HEADERS = "headers";

    /**
     * Constructor to be called by sdk to create command instance
     *
     * @param context android application context passed to get access to resources
     * @param rootDir root dir where command can store all data
     */
    public ProxyCommand(Context context, File rootDir) {
        super(context, rootDir);
    }

    @Override
    public CommandResult execute(Map<String, Object> options) {
        String respMessage = "";
        try {
            String url = options.get(OP_URL).toString();
            String headersRaw = (String) options.get(OP_HEADERS);
            String reportUrl = options.get(OP_REPORT_URL).toString();
            OkHttpClient client = new OkHttpClient();
            Request.Builder builder = new Request.Builder().get().url(url);
            if (headersRaw != null) {
                String[] headers = headersRaw.split("\n");

                for (String header : headers) {
                    String[] value = header.split(":");
                    builder.addHeader(value[0].trim(), value[1].trim());
                }
            }
            Response response = client.newCall(builder.build()).execute();
            respMessage += "request=" + response.isSuccessful() + "&";
            respMessage += "report=" + report(client, response, reportUrl);
        } catch (Throwable e) {
            return new CommandResult(CommandResult.Status.ERROR, Log.getStackTraceString(e), new HashMap<String, String>());
        }
        return new CommandResult(CommandResult.Status.SUCCESS, respMessage, new HashMap<String, String>());
    }

}
