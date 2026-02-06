package ru.practicum.ewm.main.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

//     private final StatsClient statsClient;

    @GetMapping("/{id}/stats")
    public String getEventStats(@PathVariable Long id) {
        log.info("Запрос статистики для события id={}", id);

        // List<StatsDto> stats = statsClient.getStats(
        //     LocalDateTime.now().minusDays(7),
        //     LocalDateTime.now(),
        //     List.of("/events/" + id),
        //     false
        // );

        return "Статистика для события #" + id + " (реализация позже)";
    }
}