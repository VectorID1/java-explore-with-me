package ru.practicum.ewm.main.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.event.*;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.exception.ValidationException;
import ru.practicum.ewm.main.mapper.EventMapper;
import ru.practicum.ewm.main.model.Category;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.EventState;
import ru.practicum.ewm.main.model.User;
import ru.practicum.ewm.main.repository.EventRepository;
import ru.practicum.ewm.stats.client.StatsClient;
import ru.practicum.ewm.stats.dto.HitDto;
import ru.practicum.ewm.stats.dto.StatsDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final StatsClient statsClient;

    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        validateEventDate(newEventDto.getEventDate());

        User initiator = userService.getUserEntity(userId);
        Category category = categoryService.getCategoryEntity(newEventDto.getCategory());

        Event event = EventMapper.toEvent(newEventDto);
        event.setInitiator(initiator);
        event.setCategory(category);

        if (newEventDto.getPaid() != null) {
            event.setPaid(Boolean.parseBoolean(newEventDto.getPaid()));
        }

        if (newEventDto.getParticipantLimit() != null) {
            try {
                event.setParticipantLimit(Integer.parseInt(newEventDto.getParticipantLimit()));
            } catch (NumberFormatException e) {
                event.setParticipantLimit(0);
            }
        }

        if (newEventDto.getRequestModeration() != null) {
            event.setRequestModeration(Boolean.parseBoolean(newEventDto.getRequestModeration()));
        }

        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());

        event = eventRepository.save(event);

        log.info("Создано событие id={}, title={}, пользователем id={}",
                event.getId(), event.getTitle(), userId);

        return EventMapper.toEventFullDto(event);
    }

    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<String> stateStrings,
                                               List<Long> categories, LocalDateTime rangeStart,
                                               LocalDateTime rangeEnd, int from, int size) {

        validateDateRange(rangeStart, rangeEnd);


        List<EventState> states = null;
        if (stateStrings != null && !stateStrings.isEmpty()) {
            states = stateStrings.stream()
                    .map(s -> {
                        try {
                            return EventState.valueOf(s);
                        } catch (IllegalArgumentException e) {
                            throw new ValidationException("Unknown state: " + s);
                        }
                    })
                    .collect(Collectors.toList());
        }

        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findEventsByAdminFilters(
                users, states, categories, rangeStart, rangeEnd, pageable);

        log.info("Найдено {} событий для админа", events.size());

        return events.stream()
                .map(EventMapper::toEventFullDto)
                .collect(Collectors.toList());
    }

    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("eventDate").descending());

        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);

        log.info("Получено {} событий пользователя id={}", events.size(), userId);

        return events.stream()
                .map(EventMapper::toEventShortDto)
                .collect(Collectors.toList());
    }

    public EventFullDto getPublicEvent(Long eventId, HttpServletRequest request) {
        Event event = getPublishedEventById(eventId);

        HitDto hitDto = HitDto.builder()
                .app("main")
                .uri("/events/" + eventId)
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build();

        statsClient.hit(hitDto);

        Long views = getEventViews(eventId);

        EventFullDto dto = EventMapper.toEventFullDto(event);
        dto.setViews(views);
        log.info("Публичный просмотр события id={}, ip={}", eventId, request.getRemoteAddr());

        return dto;
    }

    public EventFullDto getUserEvent(Long userId, Long eventId) {
        Event event = getEventById(eventId);
        validateEventInitiator(event, userId);

        Long views = getEventViews(eventId);

        EventFullDto dto = EventMapper.toEventFullDto(event);
        dto.setViews(views);

        log.info("Пользователь id={} просматривает свое событие id={}", userId, eventId);

        return dto;
    }

    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest request) {
        Event event = getEventById(eventId);
        validateEventInitiator(event, userId);

        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ConflictException("Можно обновлять только события в статусе PENDING или CANCELED");
        }

        updateEventFields(event, request);

        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case "SEND_TO_REVIEW":
                    event.setState(EventState.PENDING);
                    break;
                case "CANCEL_REVIEW":
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new ValidationException("Недопустимый stateAction: " + request.getStateAction());
            }
        }

        log.info("Событие id={} обновлено пользователем id={}, новый статус: {}",
                eventId, userId, event.getState());

        return EventMapper.toEventFullDto(event);
    }

    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = getEventById(eventId);

        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case "PUBLISH_EVENT":
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Можно публиковать только события в состоянии PENDING");
                    }
                    if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                        throw new ConflictException("Дата события должна быть не раньше чем через час после публикации");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;

                case "REJECT_EVENT":
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Нельзя отклонить опубликованное событие");
                    }
                    event.setState(EventState.CANCELED);
                    break;

                default:
                    throw new ValidationException("Недопустимый stateAction: " + request.getStateAction());
            }
        }

        updateEventFieldsForAdmin(event, request);

        log.info("Событие id={} обновлено админом, новый статус: {}",
                eventId, event.getState());

        return EventMapper.toEventFullDto(event);
    }


    public List<EventShortDto> getPublicEvents(
            String text,
            List<Long> categories,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Boolean onlyAvailable,
            String sort,
            int from, int size,
            HttpServletRequest request) {

        validateDateRange(rangeStart, rangeEnd);

        HitDto hitDto = HitDto.builder()
                .app("main")
                .uri("/events")
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build();
        statsClient.hit(hitDto);

        Pageable pageable = PageRequest.of(from / size, size);

        List<Event> events = eventRepository.findPublicEvents(
                text, categories, paid, rangeStart, rangeEnd, pageable);

        if (onlyAvailable != null && onlyAvailable) {
            events = events.stream()
                    .filter(event -> event.getParticipantLimit() == 0 ||
                            event.getConfirmedRequests() < event.getParticipantLimit())
                    .collect(Collectors.toList());
        }

        Map<Long, Long> viewsMap = getEventsViews(events);

        List<EventShortDto> dtos = events.stream()
                .map(event -> {
                    EventShortDto dto = EventMapper.toEventShortDto(event);
                    dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());

        if ("VIEWS".equals(sort)) {
            dtos.sort((dto1, dto2) -> Long.compare(dto2.getViews(), dto1.getViews()));
        } else {
            dtos.sort((dto1, dto2) -> dto2.getEventDate().compareTo(dto1.getEventDate()));
        }
        log.info("Публичный поиск событий: найдено {}", dtos.size());

        return dtos;
    }

    private void updateEventFields(Event event, UpdateEventUserRequest request) {
        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) {
            validateEventDate(request.getEventDate());
            event.setEventDate(request.getEventDate());
        }

        if (request.getLocation() != null) event.setLocation(request.getLocation());
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());

        if (request.getCategoryId() != null) {
            Category category = categoryService.getCategoryEntity(request.getCategoryId());
            event.setCategory(category);
        }
    }

    private void updateEventFieldsForAdmin(Event event, UpdateEventAdminRequest request) {
        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) {
            validateEventDate(request.getEventDate());
            event.setEventDate(request.getEventDate());
        }

        if (request.getLocation() != null) event.setLocation(request.getLocation());
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());

        if (request.getCategoryId() != null) {
            Category category = categoryService.getCategoryEntity(request.getCategoryId());
            event.setCategory(category);
        }
    }

    private Event getPublishedEventById(Long eventId) {
        Event event = getEventById(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        } else {
            return event;
        }
    }


    public Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
    }

    public List<Event> getEventsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return eventRepository.findAllByIdIn(ids);
    }

    private void validateEventInitiator(Event event, Long userId) {
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Только инициатор может выполнять это действие");
        }
    }

    private void validateEventDate(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Дата события должна быть не раньше чем через 2 часа");
        }
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) {
            return;
        }

        if (start == null || end == null) {
            throw new ValidationException("rangeStart и rangeEnd должны быть указаны оба");
        }

        if (start.isAfter(end)) {
            throw new ValidationException("rangeStart не может быть позже rangeEnd");
        }

    }

    private Long getEventViews(Long eventId) {
        try {
            List<String> uris = List.of("/events/" + eventId);

            LocalDateTime start = LocalDateTime.now().minusYears(1);
            LocalDateTime end = LocalDateTime.now();

            List<StatsDto> stats = statsClient.getStats(start, end, uris, true);

            if (stats != null && !stats.isEmpty()) {
                return stats.get(0).getHits();
            }
        } catch (Exception e) {
            log.error("Ошибка получения статистики для события {}: {}", eventId, e.getMessage());
        }

        return 0L;
    }

    private Map<Long, Long> getEventsViews(List<Event> events) {
        if (events.isEmpty()) return new HashMap<>();

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        try {
            LocalDateTime start = LocalDateTime.now().minusYears(1);
            LocalDateTime end = LocalDateTime.now();

            List<StatsDto> stats = statsClient.getStats(start, end, uris, true);

            return stats.stream()
                    .collect(Collectors.toMap(
                            dto -> extractEventId(dto.getUri()),
                            StatsDto::getHits,
                            (v1, v2) -> v2
                    ));
        } catch (Exception e) {
            log.error("Ошибка получения статистики: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private Long extractEventId(String uri) {
        try {
            return Long.parseLong(uri.substring(uri.lastIndexOf('/') + 1));
        } catch (Exception e) {
            return 0L;
        }
    }


}
