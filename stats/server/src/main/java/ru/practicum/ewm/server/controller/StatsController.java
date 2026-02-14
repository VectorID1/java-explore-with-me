package ru.practicum.ewm.server.controller;


import jakarta.validation.Valid;
import ru.practicum.ewm.server.exception.BadRequestException;
import ru.practicum.ewm.stats.dto.HitDto;
import ru.practicum.ewm.stats.dto.StatsDto;
import ru.practicum.ewm.server.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping
public class StatsController {

    private final StatsService statsService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveHit(@Valid @RequestBody HitDto hitDto) {
        log.info("Получен запрос на сохранение hit: app={}, uri={}, ip={}, timestamp={}",
                hitDto.getApp(), hitDto.getUri(),
                hitDto.getIp(), hitDto.getTimestamp());

        statsService.saveHit(hitDto);
    }

    @GetMapping("/stats")
    public List<StatsDto> getStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique) {

        if (start == null || end == null) {
            throw new BadRequestException("start и end не указаны");
        }
        if (start.isAfter(end)) {
            throw new BadRequestException("start должно быть раньше end");
        }

        log.info("Получен запрос на статистику: start={}, end={}, uris={}, unique={}",
                start, end, uris, unique);

        return statsService.getStats(start, end, uris, unique);
    }
}