package com.trabajaya.shared;

import com.trabajaya.storage.StorageException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Stable error shape; the existing frontend can continue reading message/fields. */
@RestControllerAdvice
public class ApiErrors {
    private static final Logger log = LoggerFactory.getLogger(ApiErrors.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> domain(ApiException error, HttpServletRequest request) {
        return response(error.status, error.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException error, HttpServletRequest request) {
        var fields = new LinkedHashMap<String, String>();
        error.getBindingResult().getFieldErrors().forEach(
                field -> fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "Revisa los campos del formulario.", request, fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class})
    ResponseEntity<?> format(Exception error, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "El formato de los datos no es valido.", request, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<?> size(Exception error, HttpServletRequest request) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "La imagen no debe superar 5 MB.", request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict(Exception error, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "El registro ya existe o entra en conflicto con los datos actuales.", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<?> denied(Exception error, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "No tienes permiso para esta accion.", request, null);
    }

    @ExceptionHandler(StorageException.class)
    ResponseEntity<?> storage(StorageException error, HttpServletRequest request) {
        HttpStatus status = switch (error.reason()) {
            case MISSING -> HttpStatus.NOT_FOUND;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case REJECTED -> HttpStatus.BAD_GATEWAY;
        };
        log.warn("Storage failure: {}", error.reason());
        return response(status, error.getMessage(), request, null);
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    ResponseEntity<?> database(Exception error, HttpServletRequest request) {
        log.warn("Database temporarily unavailable; requestId={}", request.getAttribute(RequestIdFilter.ATTRIBUTE));
        return response(HttpStatus.SERVICE_UNAVAILABLE, "La base de datos no esta disponible. Intenta nuevamente.", request, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<?> other(Exception error, HttpServletRequest request) {
        // Preserve framework 404/405/415 errors instead of converting every exception to 500.
        if (error instanceof ErrorResponse webError && webError.getStatusCode().is4xxClientError()) {
            return response(HttpStatus.valueOf(webError.getStatusCode().value()),
                    "La ruta, el metodo o el contenido de la solicitud no es valido.", request, null);
        }
        String id = correlation(request);
        log.error("Unexpected error; requestId={}", id, error);
        return response(HttpStatus.INTERNAL_SERVER_ERROR,
                "No fue posible completar la solicitud. Referencia: " + id, request, null);
    }

    private String correlation(HttpServletRequest request) {
        Object value = request.getAttribute(RequestIdFilter.ATTRIBUTE);
        if (value != null) return value.toString();
        String generated = UUID.randomUUID().toString();
        request.setAttribute(RequestIdFilter.ATTRIBUTE, generated);
        return generated;
    }

    private ResponseEntity<?> response(HttpStatus status, String message,
                                       HttpServletRequest request, Map<String, String> fields) {
        var body = new LinkedHashMap<String, Object>();
        body.put("message", message);
        body.put("status", status.value());
        body.put("code", status.name());
        body.put("requestId", correlation(request));
        if (fields != null) body.put("fields", fields);
        var builder = ResponseEntity.status(status);
        if (status == HttpStatus.TOO_MANY_REQUESTS) builder.header("Retry-After", "60");
        return builder.body(body);
    }
}
