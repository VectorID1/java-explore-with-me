package ru.practicum.ewm.main.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

    @GetMapping("/{id}/stats")
    public String getEventStats(@PathVariable Long id) {
        log.info("Запрос статистики для события id={}", id);

        return "Статистика для события #" + id + " (реализация позже)";
    }
}