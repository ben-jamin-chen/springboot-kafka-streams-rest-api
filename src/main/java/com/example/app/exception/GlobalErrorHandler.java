package com.example.app.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

@Component
public class GlobalErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalErrorHandler.class);

    public ServerResponse handle(Throwable ex, ServerRequest request) {
        return switch (ex) {
            case MovieNotFoundException notFound -> problem(HttpStatus.NOT_FOUND, notFound.getMessage());
            case NumberFormatException badId -> problem(HttpStatus.BAD_REQUEST, "movieId must be a number");
            default -> {
                logger.error("Request {} failed", request.path(), ex);
                yield problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
            }
        };
    }

    private static ServerResponse problem(HttpStatus status, String detail) {
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
