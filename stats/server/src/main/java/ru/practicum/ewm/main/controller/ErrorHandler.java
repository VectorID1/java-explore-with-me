package ru.practicum.ewm.main.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException e) {
        log.error("Ошибка валидации: {}", e.getMessage());
        return Map.of(
                "status", "BAD_REQUEST",
                "reason", "Некорректные параметры запроса",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException e) {
        log.error("Ошибка валидации: {}", e.getMessage());
        return Map.of(
                "status", "BAD_REQUEST",
                "reason", "Некорректные данные",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleAllExceptions(Exception e) {
        log.error("Внутренняя ошибка сервера: {}", e.getMessage(), e);
        return Map.of(
                "status", "INTERNAL_SERVER_ERROR",
                "reason", "Внутренняя ошибка сервера",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        );
    }
}
