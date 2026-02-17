package ru.practicum.ewm.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.main.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.main.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.exception.ValidationException;
import ru.practicum.ewm.main.mapper.RequestMapper;
import ru.practicum.ewm.main.model.*;
import ru.practicum.ewm.main.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RequestService {

    private final RequestRepository requestRepository;
    private final UserService userService;
    private final EventService eventService;

    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {

        User requester = userService.getUserEntity(userId);

        Event event = eventService.getEventById(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может подать заявку на участие");
        }


        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Вы уже подали заявку на это событие");
        }

        Request request = Request.builder()
                .requester(requester)
                .event(event)
                .created(LocalDateTime.now())
                .status(RequestStatus.PENDING)
                .build();

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
            incrementConfirmedRequests(event);
        }

        request = requestRepository.save(request);

        log.info("Создана заявка id={} на событие id={} от пользователя id={} со статусом {}",
                request.getId(), eventId, userId, request.getStatus());

        return RequestMapper.toParticipationRequestDto(request);
    }

    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        userService.getUserEntity(userId);
        List<ParticipationRequestDto> requests = requestRepository.findByRequesterId(userId).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());

        log.info("Получено {} заявок пользователя id={}", requests.size(), userId);

        return requests;
    }

    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        userService.getUserEntity(userId);
        Event event = eventService.getEventById(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Только владелец может просматривать заявки на событие");
        }

        List<ParticipationRequestDto> requests = requestRepository.findByEventId(eventId).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());

        log.info("Владелец id={} получил {} заявок на событие id={}", userId, requests.size(), eventId);

        return requests;
    }

    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Заявка не найдена"));

        if (!request.getRequester().getId().equals(userId)) {
            throw new ConflictException("Только владелец может отменить заявку");
        }

        request.setStatus(RequestStatus.CANCELED);

        log.info("Пользователь id={} отменил заявку id={} на событие id={}",
                userId, requestId, request.getEvent().getId());

        return RequestMapper.toParticipationRequestDto(request);
    }

    @Transactional
    public EventRequestStatusUpdateResult updateRequestsStatus(
            Long userId,
            Long eventId,
            EventRequestStatusUpdateRequest request) {

        Event event = eventService.getEventById(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Только владелец может изменять статус заявок");
        }

        List<Request> requests = requestRepository.findAllById(request.getRequestIds());

        for (Request req : requests) {
            if (!req.getEvent().getId().equals(eventId)) {
                throw new ConflictException("Заявка " + req.getId() + " не относится к событию " + eventId);
            }
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        RequestStatus status;
        try {
            status = RequestStatus.valueOf(request.getStatus());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Недопустимый статус: " + request.getStatus());
        }

        if (status != RequestStatus.CONFIRMED && status != RequestStatus.REJECTED) {
            throw new ValidationException("Статус должен быть CONFIRMED или REJECTED");
        }

        if (RequestStatus.CONFIRMED == status) {
            int availableSlots = event.getParticipantLimit() - event.getConfirmedRequests();
            if (event.getParticipantLimit() > 0 && requests.size() > availableSlots) {
                throw new ConflictException("Недостаточно свободных мест");
            }

            for (Request req : requests) {
                if (req.getStatus() != RequestStatus.PENDING) {
                    throw new ConflictException("Можно подтверждать только заявки в статусе PENDING");
                }
                req.setStatus(RequestStatus.CONFIRMED);
                incrementConfirmedRequests(event);
                confirmed.add(RequestMapper.toParticipationRequestDto(req));
            }
        } else if (RequestStatus.REJECTED == status) {
            for (Request req : requests) {
                if (req.getStatus() != RequestStatus.PENDING) {
                    throw new ConflictException("Можно отклонять только заявки в статусе PENDING");
                }
                req.setStatus(RequestStatus.REJECTED);
                rejected.add(RequestMapper.toParticipationRequestDto(req));
            }
        }
        log.info("Владелец id={} обновил статусы заявок на событие id={}: подтверждено={}, отклонено={}",
                userId, eventId, confirmed.size(), rejected.size());

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed)
                .rejectedRequests(rejected)
                .build();
    }

    public void incrementConfirmedRequests(Event event) {

        if (event.getParticipantLimit() > 0) {
            if (event.getConfirmedRequests() >= event.getParticipantLimit()) {
                throw new ConflictException(
                        "Достигнут лимит участников для события " + event.getTitle() + ". Максимум: " +
                                event.getParticipantLimit());
            }
        }

        event.setConfirmedRequests(event.getConfirmedRequests() + 1);

        log.info("Увеличен счетчик подтвержденных заявок для события {}: {}/{}",
                event.getId(),
                event.getConfirmedRequests(),
                event.getParticipantLimit() == 0 ? "∞" : event.getParticipantLimit());
    }


}
