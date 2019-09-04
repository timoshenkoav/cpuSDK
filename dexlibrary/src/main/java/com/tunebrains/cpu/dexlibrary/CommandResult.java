package com.tunebrains.cpu.dexlibrary;

import java.util.Map;

/**
 * Class to return result from command
 */
public class CommandResult {

    private final Status status;
    private final String message;
    private final Map<String, String> extras;

    public CommandResult(final Status status, final String message, final Map<String, String> extras) {
        this.status = status;
        this.message = message;
        this.extras = extras;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getExtras() {
        return extras;
    }

    public enum Status {
        SUCCESS,
        ERROR
    }
}
