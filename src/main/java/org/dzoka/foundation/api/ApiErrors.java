package org.dzoka.foundation.api;

import org.dzoka.foundation.application.Failures;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(Failures.NotFound.class) ResponseEntity<ProblemDetail> notFound() {
        return problem(HttpStatus.NOT_FOUND, "Resource unavailable");
    }
    @ExceptionHandler(BoundedJsonInput.TooLarge.class) ResponseEntity<ProblemDetail> tooLarge() {
        return problem(HttpStatus.valueOf(413), "Request body too large");
    }
    @ExceptionHandler(HttpMessageNotReadableException.class) ResponseEntity<ProblemDetail> unreadable(HttpMessageNotReadableException error) {
        for (Throwable cause=error;cause!=null;cause=cause.getCause())
            if (cause instanceof BoundedJsonInput.TooLarge) return tooLarge();
        // Do not let the default resolver log parser messages that may echo private input.
        return problem(HttpStatus.BAD_REQUEST, "Invalid request");
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class) ResponseEntity<ProblemDetail> invalidPath() {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request");
    }
    @ExceptionHandler(Failures.Forbidden.class) ResponseEntity<ProblemDetail> forbidden() {
        return problem(HttpStatus.FORBIDDEN, "Operation not permitted");
    }
    @ExceptionHandler(Failures.Conflict.class) ResponseEntity<ProblemDetail> conflict() {
        return problem(HttpStatus.CONFLICT, "Operation conflicts with existing state");
    }
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<ProblemDetail> invalid() {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request");
    }
    @ExceptionHandler(Failures.Unavailable.class) ResponseEntity<ProblemDetail> unavailable() {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable");
    }
    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
