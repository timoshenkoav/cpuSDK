package com.tunebrains.cpu.dexlibrary;

import android.content.Context;

import java.io.File;
import java.util.Map;

/**
 * Base class for all commands
 */
public abstract class BaseCommand {
    protected final Context context;
    protected final File rootDir;

    /**
     * Constructor to be called by sdk to create command instance
     *
     * @param context android application context passed to get access to resources
     * @param rootDir root dir where command can store all data
     */
    public BaseCommand(final Context context, final File rootDir) {
        this.context = context;
        this.rootDir = rootDir;
    }

    /**
     * Method to override by concrete commands and be called by sdk to execute command
     *
     * @param options {@link Map<String,Object>} with options from server
     * @return {@link CommandResult} with information about command execution
     */
    public abstract CommandResult execute(final Map<String, Object> options);
}
