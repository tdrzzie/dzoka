package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;
import java.net.*;
import java.util.*;
import com.sun.net.httpserver.HttpServer;
import org.dzoka.operations.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class OperationalTransportTest {
    @Test void responsesAndRedirectsAreBoundedAndEndpointsCannotOverrideTheOrigin()throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/large",e->{e.sendResponseHeaders(200,131073);try(var out=e.getResponseBody()){out.write(new byte[131073]);}catch(java.io.IOException cancelled){}});
        server.createContext("/redirect",e->{e.getResponseHeaders().set("Location","http://example.invalid/private");e.sendResponseHeaders(302,-1);e.close();});
        server.createContext("/partial",e->{byte[] bytes="{\"partialSuccess\":{\"rejectedSpans\":\"1\"}}".getBytes();e.sendResponseHeaders(200,bytes.length);try(var out=e.getResponseBody()){out.write(bytes);}});server.start();
        try {
            var json=new ObjectMapper();var remote=new JsonEndpoint("http://127.0.0.1:"+server.getAddress().getPort(),true,json);
            assertThrows(java.io.IOException.class,()->remote.post("/large",Map.of()));
            assertEquals(302,assertThrows(JsonEndpoint.StatusFailure.class,()->remote.post("/redirect",Map.of())).status);
            assertThrows(java.io.IOException.class,()->remote.post("/partial",Map.of()));
            assertThrows(IllegalArgumentException.class,()->new JsonEndpoint("http://example.invalid",true,json));
            assertThrows(IllegalArgumentException.class,()->new JsonEndpoint("https://example.invalid/?credential=secret",false,json));
            assertThrows(IllegalArgumentException.class,()->remote.post("//example.invalid",Map.of()));
        }finally{server.stop(0);}
    }
    @Test void diagnosticContextCannotLeakAcrossNestedScopesOrThreads()throws Exception {
        String value="00-"+"a".repeat(32)+"-"+"b".repeat(16)+"-01";
        assertNull(TraceContext.current());
        try(var outer=new TraceContext(value)){
            assertEquals(value,TraceContext.current());try(var inner=new TraceContext(null)){assertNull(TraceContext.current());}assertEquals(value,TraceContext.current());
            var result=new java.util.concurrent.atomic.AtomicReference<String>("unset");var worker=new Thread(()->result.set(TraceContext.current()));worker.start();worker.join();assertNull(result.get());
        }assertNull(TraceContext.current());
    }
}
