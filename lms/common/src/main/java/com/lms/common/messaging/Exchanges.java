package com.lms.common.messaging;

public final class Exchanges {
    private Exchanges() {}
    public static final String EVENTS   = "lms.events";    // domain events (pub/sub)
    public static final String COMMANDS = "lms.commands";  // saga commands
    public static final String DLX      = "lms.dlx";       // dead letters
}
