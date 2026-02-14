package ru.practicum.ewm.main.controller.privateControllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.dto.event.EventFullDto;
import ru.practicum.ewm.main.dto.event.EventShortDto;
import ru.practicum.ewm.main.dto.event.NewEventDto;
import ru.practicum.ewm.main.dto.event.UpdateEventUserRequest;
import ru.practicum.ewm.main.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.main.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.main.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.main.service.EventService;
import ru.practicum.ewm.main.service.RequestService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
public class PrivateEventController {

    private final EventService eventService;
    private final RequestService requestService;

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(@PathVariable Long userId, @Valid @RequestBody NewEventDto newEventDto) {
        log.info("POST /users/{userId}/events- создание события: {}", newEventDto.getAnnotation());

        return eventService.createEvent(userId, newEventDto);
    }

    @GetMapping("/events")
    public List<EventShortDto> getUserEvents(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int from,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        log.info("GET /users/{userId}/events- получение списка события пользователя c Id: {}", userId);

        return eventService.getUserEvents(userId, from, size);
    }

    @GetMapping("/events/{eventId}")
    public EventFullDto getUserEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId) {
        log.info("GET /users/{userId}/events/{eventId} - получение события c Id {} пользователя c Id: {}", eventId, userId);

        return eventService.getUserEvent(userId, eventId);
    }

    @PatchMapping("events/{eventId}")
    public EventFullDto updateEventForUser(@PathVariable Long userId,
                                           @PathVariable Long eventId,
                                           @Valid @RequestBody UpdateEventUserRequest request) {
        log.info("Patch /users/{userId}/events/{eventId} - обновление события c Id {} пользователя c Id: {}", eventId, userId);

        return eventService.updateEventByUser(userId, eventId, request);
    }


    @PatchMapping("events/{eventId}/requests")
    public EventRequestStatusUpdateResult updateRequestsStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody EventRequestStatusUpdateRequest request) {

        log.info("PATCH /users/{}/events/{}/requests - обновление статуса заявок", userId, eventId);
        return requestService.updateRequestsStatus(userId, eventId, request);
    }

    @GetMapping("events/{eventId}/requests")
    public List<ParticipationRequestDto> getEventRequests(
            @PathVariable Long userId,
            @PathVariable Long eventId) {

        return requestService.getEventRequests(userId, eventId);
    }

    @GetMapping("/requests")
    public List<ParticipationRequestDto> getUserRequests(@PathVariable Long userId) {
        log.info("GET /users/{}/requests", userId);
        return requestService.getUserRequests(userId);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(@PathVariable Long userId,
                                                 @RequestParam Long eventId) {
        log.info("POST / users/{}/requests запрос на участие в событии с id {}", userId, eventId);

        return requestService.createRequest(userId, eventId);
    }

    @PatchMapping("/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable Long userId,
                                                 @PathVariable Long requestId) {
        log.info("PATCH /users/{}/requests/{}/cancel отмена запроса на участие", userId, requestId);
        return requestService.cancelRequest(userId, requestId);
    }

}
