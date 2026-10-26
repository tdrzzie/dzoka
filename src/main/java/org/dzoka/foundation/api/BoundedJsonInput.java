package org.dzoka.foundation.api;

import java.io.*;
import java.lang.reflect.Type;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

/** Foundation JSON APIs only. Future media uploads use their own isolated streaming boundary. */
@ControllerAdvice
public final class BoundedJsonInput extends RequestBodyAdviceAdapter {
    private static final int LIMIT = 16 * 1024;
    public static final class TooLarge extends RuntimeException {}
    @Override public boolean supports(MethodParameter method, Type target, Class<? extends HttpMessageConverter<?>> converter) {
        return true;
    }
    @Override public HttpInputMessage beforeBodyRead(HttpInputMessage input, MethodParameter method,
            Type target, Class<? extends HttpMessageConverter<?>> converter) throws IOException {
        if (input.getHeaders().getContentLength() > LIMIT) throw new TooLarge();
        var bounded = new FilterInputStream(input.getBody()) {
            private int consumed;
            @Override public int read() throws IOException {
                int result = in.read();
                if (result >= 0 && ++consumed > LIMIT) throw new TooLarge();
                return result;
            }
            @Override public int read(byte[] bytes, int offset, int length) throws IOException {
                int result = in.read(bytes, offset, Math.min(length, LIMIT-consumed+1));
                if (result > 0 && (consumed += result) > LIMIT) throw new TooLarge();
                return result;
            }
        };
        return new HttpInputMessage() {
            @Override public InputStream getBody() { return bounded; }
            @Override public HttpHeaders getHeaders() { return input.getHeaders(); }
        };
    }
}
