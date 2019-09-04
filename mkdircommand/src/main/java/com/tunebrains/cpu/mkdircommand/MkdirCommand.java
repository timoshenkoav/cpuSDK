package com.tunebrains.cpu.mkdircommand;

import android.content.Context;

import com.tunebrains.cpu.dexlibrary.BaseCommand;
import com.tunebrains.cpu.dexlibrary.CommandResult;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MkdirCommand extends BaseCommand {

    public static final String OP_DIR_NAME = "dirName";

    /**
     * Constructor to be called by sdk to create command instance
     *
     * @param context android application context passed to get access to resources
     * @param rootDir root dir where command can store all data
     */
    public MkdirCommand(Context context, File rootDir) {
        super(context, rootDir);
    }

    @Override
    public CommandResult execute(Map<String, Object> options) {
        String name = options.get(OP_DIR_NAME).toString();
        File f = new File(rootDir, name);
        boolean res = f.mkdirs();
        if (res) {
            return new CommandResult(CommandResult.Status.SUCCESS, "ok", new HashMap<String, String>());
        }
        return new CommandResult(CommandResult.Status.ERROR, "failed to create dir", new HashMap<String, String>());
    }
}
