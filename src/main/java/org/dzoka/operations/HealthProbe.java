package org.dzoka.operations;
/** Container probe without a shell, network utilities or authentication secrets. */
public final class HealthProbe {
    public static void main(String[] args)throws Exception {
        var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:8080/health/ready")).timeout(java.time.Duration.ofSeconds(5)).build();
        int code=java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(2)).build().send(request,java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode();
        if(code!=200)System.exit(1);
    }
    private HealthProbe() {}
}
