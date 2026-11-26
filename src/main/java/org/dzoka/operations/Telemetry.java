package org.dzoka.operations;

import jakarta.annotation.PreDestroy;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Minimal OTLP/HTTP JSON producer. Bounded and lossy; domain audit stays in PostgreSQL. */
@Component
public final class Telemetry {
    private final JsonEndpoint remote;private final ThreadPoolExecutor worker;private final SecureRandom random=new SecureRandom();
    private final LongAdder requests=new LongAdder(),errors=new LongAdder(),operations=new LongAdder(),dropped=new LongAdder(),exportFailures=new LongAdder();
    public Telemetry(ObjectMapper json,Environment env){
        String origin=env.getProperty("dzoka.otlp.origin","");
        remote=origin.isBlank()?null:new JsonEndpoint(origin,env.getProperty("dzoka.operations.allow-insecure-loopback",Boolean.class,false),json,env.getProperty("dzoka.otlp.token-file",""));
        worker=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(128),r->{Thread t=new Thread(r,"dzoka-safe-telemetry");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    }
    public record Context(String trace,String parent,String span) {}
    public Context context(String header){
        String trace=null,parent=null;
        if(header!=null&&header.matches("00-[0-9a-f]{32}-[0-9a-f]{16}-0[01]")&&!header.substring(3,35).equals("0".repeat(32))&&!header.substring(36,52).equals("0".repeat(16))){trace=header.substring(3,35);parent=header.substring(36,52);}
        return new Context(trace==null?id(16):trace,parent,id(8));
    }
    private String id(int size){byte[] bytes=new byte[size];random.nextBytes(bytes);return HexFormat.of().formatHex(bytes);}
    public void record(Context context,String method,String route,int status,Instant start,long nanos){
        boolean internal=method.equals("INTERNAL");if(internal)operations.increment();else requests.increment();if(status>=500)errors.increment();if(remote==null)return;
        // Route is a framework template, never the request URI, query or path-variable values.
        if(!route.matches("/[A-Za-z0-9_/{}/.-]{0,180}"))route="/UNMATCHED";
        if(!Set.of("GET","POST","PUT","DELETE","PATCH","HEAD","OPTIONS","INTERNAL").contains(method))method="OTHER";
        long epochNanos=Math.addExact(Math.multiplyExact(start.getEpochSecond(),1_000_000_000L),start.getNano());
        var attributes=internal?List.of(attribute("dzoka.operation",route),attribute("dzoka.result",status>=500?"UNAVAILABLE":status>=400?"DENIED":"COMPLETE")):List.of(attribute("http.request.method",method),attribute("http.route",route),Map.<String,Object>of("key","http.response.status_code","value",Map.of("intValue",Integer.toString(status))));
        var span=new LinkedHashMap<String,Object>();span.put("traceId",context.trace());span.put("spanId",context.span());if(context.parent()!=null)span.put("parentSpanId",context.parent());
        span.put("name",method+" "+route);span.put("kind",internal?1:2);span.put("startTimeUnixNano",Long.toString(epochNanos));span.put("endTimeUnixNano",Long.toString(epochNanos+Math.max(0,nanos)));span.put("attributes",attributes);span.put("status",Map.of("code",status>=500?2:1));
        Map<String,Object> resource=Map.of("attributes",List.of(attribute("service.name","dzoka-core")));
        var payload=Map.of("resourceSpans",List.of(Map.of("resource",resource,"scopeSpans",List.of(Map.of("scope",Map.of("name","org.dzoka.safe-http","version","1"),"spans",List.of(span))))));
        var points=new ArrayList<Map<String,Object>>();
        for(var entry:counters().entrySet())points.add(Map.of("name","dzoka."+entry.getKey(),"sum",Map.of("aggregationTemporality",2,"isMonotonic",true,"dataPoints",List.of(Map.of("timeUnixNano",Long.toString(epochNanos),"asInt",Long.toString(entry.getValue()))))));
        var metrics=Map.of("resourceMetrics",List.of(Map.of("resource",resource,"scopeMetrics",List.of(Map.of("scope",Map.of("name","org.dzoka.safe-http"),"metrics",points)))));
        var logs=Map.of("resourceLogs",List.of(Map.of("resource",resource,"scopeLogs",List.of(Map.of("logRecords",List.of(Map.of("timeUnixNano",Long.toString(epochNanos),"traceId",context.trace(),"spanId",context.span(),"severityNumber",status>=500?17:9,"body",Map.of("stringValue",internal?"OPERATION_COMPLETED":"HTTP_REQUEST_COMPLETED"),"attributes",attributes)))))));
        try{worker.execute(()->{try{remote.post("/v1/traces",payload);remote.post("/v1/metrics",metrics);remote.post("/v1/logs",logs);}catch(Exception unavailable){if(unavailable instanceof InterruptedException)Thread.currentThread().interrupt();exportFailures.increment();}});}catch(RejectedExecutionException full){dropped.increment();}
    }
    private static Map<String,Object> attribute(String key,String value){return Map.of("key",key,"value",Map.of("stringValue",value));}
    public boolean enabled(){return remote!=null;}
    public void job(String parent,String task,String state,Instant start,Instant end){record(context(parent),"INTERNAL","/jobs/"+task+"/"+state,Set.of("FAILED","REJECTED").contains(state)?503:200,start,java.time.Duration.between(start,end).toNanos());}
    public Map<String,Long> counters(){return Map.of("httpRequests",requests.sum(),"serverErrors",errors.sum(),"coreOperations",operations.sum(),"telemetryDropped",dropped.sum(),"telemetryExportFailures",exportFailures.sum());}
    @PreDestroy public void close(){worker.shutdownNow();}
}
