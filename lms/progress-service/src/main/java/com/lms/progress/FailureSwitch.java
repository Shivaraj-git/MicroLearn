package com.lms.progress;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicBoolean;

/** TEST ONLY: lets us make the next "grant access" fail, to try the refund path. */
@Component
public class FailureSwitch {

    private final AtomicBoolean failNext = new AtomicBoolean(false);

    public void armFailNext() { failNext.set(true); }

    /** Returns true once, then resets itself. */
    public boolean consume() { return failNext.getAndSet(false); }
}
