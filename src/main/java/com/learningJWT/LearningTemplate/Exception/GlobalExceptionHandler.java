package com.learningJWT.LearningTemplate.Exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── Validation errors (from @Valid on request bodies) ──────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());
        return errorResponse(HttpStatus.BAD_REQUEST, "Validation failed", req.getRequestURI(), errors);
    }

    // ── Bean validation (path/query params) ────────────────────────────────
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest req) {
        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());
        return errorResponse(HttpStatus.BAD_REQUEST, "Constraint violation", req.getRequestURI(), errors);
    }

    // ── Access Denied ───────────────────────────────────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest req) {
        return errorResponse(HttpStatus.FORBIDDEN, "Access denied: insufficient permissions",
                req.getRequestURI(), null);
    }

    // ── Authentication failures ─────────────────────────────────────────────
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<Map<String, Object>> handleAuth(
            Exception ex, HttpServletRequest req) {
        return errorResponse(HttpStatus.UNAUTHORIZED, "Authentication failed: " + ex.getMessage(),
                req.getRequestURI(), null);
    }

    // ── File too large ──────────────────────────────────────────────────────
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleFileTooLarge(
            MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return errorResponse(HttpStatus.PAYLOAD_TOO_LARGE,
                "File size exceeds the maximum allowed limit (10 MB)", req.getRequestURI(), null);
    }

    // ── Missing required params ─────────────────────────────────────────────
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest req) {
        return errorResponse(HttpStatus.BAD_REQUEST,
                "Required parameter '" + ex.getParameterName() + "' is missing",
                req.getRequestURI(), null);
    }

    // ── Domain/Application exceptions ──────────────────────────────────────
    @ExceptionHandler(UserException.class)
    public ResponseEntity<Map<String, Object>> handleUserException(
            UserException ex, HttpServletRequest req) {
        return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI(), null);
    }

    // ── Fallback – catches everything else ─────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) msg = "An unexpected error occurred";
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, msg, req.getRequestURI(), null);
    }

    // ── Helper ─────────────────────────────────────────────────────────────
    private ResponseEntity<Map<String, Object>> errorResponse(
            HttpStatus status, String message, String path, List<String> details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        if (details != null && !details.isEmpty()) body.put("details", details);
        return ResponseEntity.status(status).body(body);
    }
}
