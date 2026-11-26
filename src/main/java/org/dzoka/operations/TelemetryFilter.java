package org.dzoka.operations;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Instant;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE+5)
public final class TelemetryFilter extends OncePerRequestFilter {
    private final Telemetry telemetry;
    public TelemetryFilter(Telemetry telemetry){this.telemetry=telemetry;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
        var context=telemetry.context(request.getHeader("traceparent"));Instant start=Instant.now();long nanos=System.nanoTime();boolean failed=true;
        response.setHeader("X-Trace-Id",context.trace());
        response.setHeader("traceparent","00-"+context.trace()+"-"+context.span()+"-01");
        try(var scope=new TraceContext("00-"+context.trace()+"-"+context.span()+"-01")){chain.doFilter(request,response);failed=false;}finally{
            Object template=request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            telemetry.record(context,request.getMethod(),template instanceof String s?s:"/UNMATCHED",failed?500:response.getStatus(),start,System.nanoTime()-nanos);
        }
    }
}
