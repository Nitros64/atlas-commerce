package com.atlascommerce.auth.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(
                        IllegalArgumentException ex,
                        HttpServletRequest request) {
                ErrorResponse response = new ErrorResponse(
                                OffsetDateTime.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                "Bad Request",
                                ex.getMessage(),
                                request.getRequestURI(),
                                List.of());

                return ResponseEntity.badRequest().body(response);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                List<String> details = ex.getBindingResult()
                                .getAllErrors()
                                .stream()
                                .map(error -> {
                                        if (error instanceof FieldError fieldError) {
                                                return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                                        }
                                        return error.getDefaultMessage();
                                })
                                .toList();

                ErrorResponse response = new ErrorResponse(
                                OffsetDateTime.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                "Validation Error",
                                "Request validation failed",
                                request.getRequestURI(),
                                details);

                return ResponseEntity.badRequest().body(response);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ErrorResponse> handleConstraintViolation(
                        ConstraintViolationException ex,
                        HttpServletRequest request) {
                List<String> details = ex.getConstraintViolations()
                                .stream()
                                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                                .toList();

                ErrorResponse response = new ErrorResponse(
                                OffsetDateTime.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                "Validation Error",
                                "Constraint violation",
                                request.getRequestURI(),
                                details);

                return ResponseEntity.badRequest().body(response);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGeneric(
                        Exception ex,
                        HttpServletRequest request) {
                ErrorResponse response = new ErrorResponse(
                                OffsetDateTime.now(),
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "Internal Server Error",
                                ex.getMessage(),
                                request.getRequestURI(),
                                List.of());

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        @ExceptionHandler(RedisOperationException.class)
        public ResponseEntity<ErrorResponse> handleRedisOperationException(RedisOperationException ex,
                        HttpServletRequest request) {
                return ResponseEntity.status(503)
                                .body(ErrorResponse.of(
                                                503,
                                                "Service Unavailable",
                                                "Temporary authentication infrastructure problem",
                                                request.getRequestURI()));
        }

        @ExceptionHandler(TooManyLoginAttemptsException.class)
        public ResponseEntity<ErrorResponse> handleTooManyLoginAttempts(TooManyLoginAttemptsException ex, HttpServletRequest request) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                .body(ErrorResponse.of(
                                                429,
                                                "Too Many Requests",
                                                ex.getMessage(),
                                                request.getRequestURI()));
        }
}