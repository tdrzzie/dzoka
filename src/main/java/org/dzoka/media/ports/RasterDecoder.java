package org.dzoka.media.ports;
public interface RasterDecoder {
    record Result(byte[] png,int width,int height,String sourceType){}
    final class Rejected extends RuntimeException{}
    Result sanitize(byte[] source);
    String policyVersion();
}
