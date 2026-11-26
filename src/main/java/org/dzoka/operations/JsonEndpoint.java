package org.dzoka.operations;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import tools.jackson.databind.*;

/** Fixed origin, no redirect, bounded response. Never includes remote bodies in errors. */
public final class JsonEndpoint {
    private final URI origin;private final ObjectMapper json;private final HttpClient http;private final String token;
    public JsonEndpoint(String origin,boolean loopback,ObjectMapper json){
        this(origin,loopback,json,"");
    }
    public JsonEndpoint(String origin,boolean loopback,ObjectMapper json,String tokenFile){
        URI uri=URI.create(origin);
        if(uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||!java.util.Set.of("","/").contains(uri.getPath())
            ||!("https".equals(uri.getScheme())||loopback&&"http".equals(uri.getScheme())&&java.util.Set.of("127.0.0.1","localhost").contains(uri.getHost())))throw new IllegalArgumentException("Secure fixed operational origin required");
        this.origin=uri;this.json=json;http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build();
        if(tokenFile.isBlank())token=null;
        else try{var file=java.nio.file.Path.of(tokenFile);if(java.nio.file.Files.size(file)>8192)throw new IllegalArgumentException("Invalid operational credential file");String value=java.nio.file.Files.readString(file).strip();if(!value.matches("[A-Za-z0-9._~+/-]{1,8000}=*"))throw new IllegalArgumentException("Invalid operational credential file");token=value;}catch(IOException unavailable){throw new IllegalArgumentException("Operational credential file unavailable");}
    }
    public JsonNode post(String path,Object body)throws IOException,InterruptedException {
        if(!path.matches("/[a-zA-Z0-9][a-zA-Z0-9./_-]*")||java.util.Arrays.stream(path.split("/")).anyMatch(p->p.equals("..")||p.equals(".")))throw new IllegalArgumentException();
        return exchange(HttpRequest.newBuilder(origin.resolve(path)).timeout(Duration.ofSeconds(3)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(body))).build());
    }
    public JsonNode get(String path,java.util.Map<String,String> query)throws IOException,InterruptedException {
        if(!path.matches("/[a-zA-Z0-9][a-zA-Z0-9./_-]*")||java.util.Arrays.stream(path.split("/")).anyMatch(p->p.equals("..")||p.equals(".")))throw new IllegalArgumentException();
        String parameters=query.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).map(e->java.net.URLEncoder.encode(e.getKey(),java.nio.charset.StandardCharsets.UTF_8)+"="+java.net.URLEncoder.encode(e.getValue(),java.nio.charset.StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&"));
        return exchange(HttpRequest.newBuilder(origin.resolve(path+"?"+parameters)).timeout(Duration.ofSeconds(3)).GET().build());
    }
    public static final class StatusFailure extends IOException {public final int status;private StatusFailure(int status){super("Operational endpoint unavailable");this.status=status;}}
    private JsonNode exchange(HttpRequest request)throws IOException,InterruptedException {
        if(token!=null)request=HttpRequest.newBuilder(request,(name,value)->!name.equalsIgnoreCase("Authorization")).header("Authorization","Bearer "+token).build();
        var response=http.send(request,info->new BoundedBody());
        if(response.statusCode()!=200)throw new StatusFailure(response.statusCode());
        byte[] bytes=response.body();var result=bytes.length==0?json.createObjectNode():json.readTree(bytes);
        for(String name:java.util.List.of("rejectedSpans","rejectedDataPoints","rejectedLogRecords"))if(result.path("partialSuccess").path(name).asLong()>0)throw new IOException("Operational endpoint rejected telemetry");
        return result;
    }
    private static final class BoundedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final HttpResponse.BodySubscriber<byte[]> delegate=HttpResponse.BodySubscribers.ofByteArray();private java.util.concurrent.Flow.Subscription subscription;private int length;
        public java.util.concurrent.CompletionStage<byte[]> getBody(){return delegate.getBody();}
        public void onSubscribe(java.util.concurrent.Flow.Subscription subscription){this.subscription=subscription;delegate.onSubscribe(subscription);}
        public void onNext(java.util.List<java.nio.ByteBuffer> buffers){for(var buffer:buffers){length+=buffer.remaining();if(length>131072){subscription.cancel();delegate.onError(new IOException("Operational response exceeds bound"));return;}}delegate.onNext(buffers);}
        public void onError(Throwable error){delegate.onError(error);}
        public void onComplete(){delegate.onComplete();}
    }
}
