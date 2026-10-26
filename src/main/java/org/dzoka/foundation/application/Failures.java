package org.dzoka.foundation.application;

public final class Failures {
    private Failures() {}
    public static final class Forbidden extends RuntimeException {}
    public static final class Conflict extends RuntimeException {}
    public static final class NotFound extends RuntimeException {}
    public static final class Unavailable extends RuntimeException {
        public Unavailable(Throwable cause) { super("Persistence unavailable", cause); }
    }
}
