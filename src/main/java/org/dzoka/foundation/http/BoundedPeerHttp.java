package org.dzoka.foundation.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Fixed-origin transport shared by institutional federation and traceability adapters. */
public final class BoundedPeerHttp {
    public record Reply(int status,byte[] body,String location) {}

    public static URI origin(String value,boolean fixture) {
        URI uri=URI.create(value);
        boolean local=fixture && "http".equals(uri.getScheme()) && "127.0.0.1".equals(uri.getHost()) && uri.getPort()>0;
        boolean secure="https".equals(uri.getScheme()) && uri.getHost()!=null;
        if(!(secure || local) || uri.getRawAuthority()==null || uri.getRawUserInfo()!=null || uri.getRawFragment()!=null
                || uri.getRawQuery()!=null || !uri.getRawPath().isEmpty())
            throw new IllegalArgumentException("Invalid institutional origin");
        return uri;
    }

    public static Reply exchange(URI origin,String route,byte[] body,Map<String,String> headers,int max) throws IOException {
        if(!route.matches("/[A-Za-z0-9/_-]+")) throw new IllegalArgumentException("Invalid institutional resource");
        var connection=(HttpURLConnection)origin.resolve(route).toURL().openConnection(Proxy.NO_PROXY);
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(1200);
        connection.setReadTimeout(1200);
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
        try {
            headers.forEach(connection::setRequestProperty);
            if(body!=null) {
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(body.length);
                try(var stream=connection.getOutputStream()) { stream.write(body); }
            }
            int status=connection.getResponseCode();
            String location=connection.getHeaderField("Location");
            if(status<200 || status>=300) return new Reply(status,new byte[0],null);
            if(connection.getContentLengthLong()>max) throw new IOException("Institutional response exceeds limit");
            try(var input=connection.getInputStream()) {
                var result=new ByteArrayOutputStream();
                byte[] buffer=new byte[2048];
                while(System.nanoTime()<deadline && !Thread.currentThread().isInterrupted()) {
                    int count=input.read(buffer,0,Math.min(buffer.length,max+1-result.size()));
                    if(count<0) return new Reply(status,result.toByteArray(),location);
                    if(result.size()+count>max) throw new IOException("Institutional response exceeds limit");
                    result.write(buffer,0,count);
                }
                throw new IOException("Institutional response deadline exceeded");
            }
        } finally { connection.disconnect(); }
    }

    private BoundedPeerHttp() {}
}
