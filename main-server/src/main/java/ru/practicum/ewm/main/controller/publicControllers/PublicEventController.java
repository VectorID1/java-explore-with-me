package ru.practicum.ewm.main.controller.publicControllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.dto.comment.CommentResponseDto;
import ru.practicum.ewm.main.dto.event.EventFullDto;
import ru.practicum.ewm.main.dto.event.EventShortDto;
import ru.practicum.ewm.main.service.CommentService;
import ru.practicum.ewm.main.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final EventService eventService;
    private final CommentService commentService;

    @GetMapping
    public List<EventShortDto> getPublicEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime rangeStart,

            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        return eventService.getPublicEvents(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size, request);

    }

    @GetMapping("/{id}")
    public EventFullDto getPublicEvent(
            @PathVariable Long id,
            HttpServletRequest request) {
        return eventService.getPublicEvent(id, request);
    }

    @GetMapping("/{eventId}/comments")
    public List<CommentResponseDto> getEventComments(@PathVariable Long eventId,
                                                     @RequestParam(defaultValue = "0") int from,
                                                     @RequestParam(defaultValue = "10") int size) {
        log.info("GET /events/{}/comments?from={}&size={} получение всех комментариев события", eventId, from, size);
        return commentService.getEventComments(eventId, from, size);
    }

    @GetMapping("/{eventId}/comments/{commentId}")
    public CommentResponseDto getComment(@PathVariable Long eventId,
                                         @PathVariable Long commentId) {
        log.info("GET /events/{}/comments/{} получение комментария по id", eventId, commentId);
        return commentService.getComment(commentId);
    }
}