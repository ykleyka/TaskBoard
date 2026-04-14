package com.ykleyka.taskboard.exception;

import com.ykleyka.taskboard.dto.error.ApiErrorResponse;
import com.ykleyka.taskboard.dto.error.ApiValidationError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final String VALIDATION_FAILED_MESSAGE = "Request validation failed";

    @ExceptionHandler({
        AsyncTaskNotFoundException.class,
        UserNotFoundException.class,
        ProjectNotFoundException.class,
        TaskNotFoundException.class,
        TagNotFoundException.class,
        CommentNotFoundException.class
    })
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            RuntimeException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFoundException(
            NoResourceFoundException exception, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Endpoint " + request.getRequestURI() + " not found",
                request,
                List.of());
    }

    @ExceptionHandler({
        UserConflictException.class,
        ProjectConflictException.class,
        DataIntegrityViolationException.class
    })
    public ResponseEntity<ApiErrorResponse> handleConflict(
            RuntimeException exception, HttpServletRequest request) {
        String message =
                exception instanceof DataIntegrityViolationException
                        ? "Operation violates data integrity constraints"
                        : exception.getMessage();
        return buildResponse(HttpStatus.CONFLICT, message, request, List.of());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException exception, HttpServletRequest request) {
        String message =
                StringUtils.hasText(exception.getReason())
                        ? exception.getReason()
                        : "Request processing failed";
        return buildResponse(exception.getStatusCode(), message, request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ApiValidationError> errors =
                exception.getBindingResult().getFieldErrors().stream()
                        .map(this::toValidationError)
                        .toList();
        return buildResponse(
                HttpStatus.BAD_REQUEST, VALIDATION_FAILED_MESSAGE, request, errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception, HttpServletRequest request) {
        List<ApiValidationError> errors = new ArrayList<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            if (result instanceof ParameterErrors parameterErrors) {
                addParameterErrors(errors, parameterErrors);
                continue;
            }
            String parameterName = getParameterName(result);
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                errors.add(toValidationError(
                        parameterName,
                        error.getDefaultMessage(),
                        result.getArgument()));
            }
        }
        return buildResponse(
                HttpStatus.BAD_REQUEST, VALIDATION_FAILED_MESSAGE, request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
            ConstraintViolationException exception, HttpServletRequest request) {
        List<ApiValidationError> errors =
                exception.getConstraintViolations().stream()
                        .map(this::toValidationError)
                        .toList();
        return buildResponse(
                HttpStatus.BAD_REQUEST, VALIDATION_FAILED_MESSAGE, request, errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        List<ApiValidationError> errors =
                List.of(toValidationError(
                        exception.getName(),
                        "has invalid format",
                        exception.getValue()));
        return buildResponse(
                HttpStatus.BAD_REQUEST, VALIDATION_FAILED_MESSAGE, request, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        log.debug("Cannot read request body", exception);
        List<ApiValidationError> errors =
                List.of(toValidationError(
                        "requestBody",
                        "body is malformed or contains invalid values",
                        null));
        return buildResponse(
                HttpStatus.BAD_REQUEST, VALIDATION_FAILED_MESSAGE, request, errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception for {}", request.getRequestURI(), exception);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected server error",
                request,
                List.of());
    }

    private void addParameterErrors(List<ApiValidationError> errors, ParameterErrors parameterErrors) {
        for (FieldError fieldError : parameterErrors.getFieldErrors()) {
            errors.add(toValidationError(
                    fieldError.getField(),
                    fieldError.getDefaultMessage(),
                    fieldError.getRejectedValue()));
        }
        if (parameterErrors.getFieldErrors().isEmpty()) {
            String parameterName = getParameterName(parameterErrors);
            for (ObjectError globalError : parameterErrors.getGlobalErrors()) {
                errors.add(toValidationError(
                        parameterName,
                        globalError.getDefaultMessage(),
                        parameterErrors.getArgument()));
            }
        }
    }

    private ApiValidationError toValidationError(FieldError error) {
        return toValidationError(error.getField(), error.getDefaultMessage(), error.getRejectedValue());
    }

    private ApiValidationError toValidationError(ConstraintViolation<?> violation) {
        return toValidationError(
                violation.getPropertyPath().toString(),
                violation.getMessage(),
                violation.getInvalidValue());
    }

    private ApiValidationError toValidationError(String field, String message, Object rejectedValue) {
        return new ApiValidationError(field, defaultMessage(message), stringify(rejectedValue));
    }

    private String getParameterName(ParameterValidationResult result) {
        String parameterName = result.getMethodParameter().getParameterName();
        return StringUtils.hasText(parameterName) ? parameterName : "parameter";
    }

    private String defaultMessage(String message) {
        return StringUtils.hasText(message) ? message : "Validation error";
    }

    private String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatusCode statusCode,
            String message,
            HttpServletRequest request,
            List<ApiValidationError> errors) {
        HttpStatus httpStatus = HttpStatus.resolve(statusCode.value());
        String error = httpStatus != null ? httpStatus.getReasonPhrase() : statusCode.toString();
        ApiErrorResponse body =
                new ApiErrorResponse(
                        Instant.now(),
                        statusCode.value(),
                        error,
                        message,
                        request.getRequestURI(),
                        errors);
        return ResponseEntity.status(statusCode).body(body);
    }
}
