package ru.practicum.ewm.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.ewm.server.exception.BadRequestException;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(BadRequestException e) {
        log.error("BadRequestException: {}", e.getMessage());
        return Map.of(
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleMissingParams(MissingServletRequestParameterException e) {
        log.error("Missing parameter: {}", e.getMessage());
        return Map.of(
                "error", "Отсутствует обязательный параметр: " + e.getParameterName(),
                "timestamp", LocalDateTime.now().toString()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleException(Exception e) {
        log.error("Internal server error: ", e);
        return Map.of(
                "error", "Внутренняя ошибка сервера",
                "timestamp", LocalDateTime.now().toString()
        );
    }
}