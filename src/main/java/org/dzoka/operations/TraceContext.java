package org.dzoka.operations;

/** Diagnostic IDs only, never tenant/authorization state. Scopes always restore the previous value. */
public final class TraceContext implements AutoCloseable {
    private static final ThreadLocal<String> CURRENT=new ThreadLocal<>();
    private final String previous;
    public TraceContext(String value){previous=CURRENT.get();if(value==null)CURRENT.remove();else if(value.matches("00-[0-9a-f]{32}-[0-9a-f]{16}-0[01]"))CURRENT.set(value);else throw new IllegalArgumentException();}
    public static String current(){return CURRENT.get();}
    @Override public void close(){if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
}
