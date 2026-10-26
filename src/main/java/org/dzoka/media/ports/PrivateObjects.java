package org.dzoka.media.ports;
public interface PrivateObjects extends AutoCloseable {
    boolean enabled();
    void put(String key,byte[] content,String sha256,String contentType);
    byte[] get(String key,String sha256,long expectedBytes,int maximumBytes);
    void delete(String key);
    @Override default void close(){}
}
