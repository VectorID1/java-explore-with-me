package ru.practicum.ewm.main.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.comment.CommentResponseDto;
import ru.practicum.ewm.main.dto.comment.NewCommentDto;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.mapper.CommentMapper;
import ru.practicum.ewm.main.model.Comment;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.User;
import ru.practicum.ewm.main.repository.CommentRepository;
import ru.practicum.ewm.main.repository.EventRepository;
import ru.practicum.ewm.main.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentService {


    private final CommentRepository commentRepository;
    private final UserService userService;
    private final EventService eventService;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Transactional
    public CommentResponseDto createComment(Long userId, Long eventId, NewCommentDto dto) {
        User author = userService.getUserEntity(userId);

        Event event = eventService.getEventById(eventId);

        Comment comment = CommentMapper.toComment(dto, event, author);

        comment = commentRepository.save(comment);

        log.info("Пользователь id={} оставил комментарий к событию id={}", userId, eventId);
        return CommentMapper.toCommentResponseDto(comment);
    }

    @Transactional
    public CommentResponseDto updateComment(Long userId, Long commentId, NewCommentDto dto) {
        Comment comment = getCommentById(commentId);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Только автор может редактировать комментарий");
        }

        comment.setText(dto.getText());
        comment.setUpdatedDate(LocalDateTime.now());
        log.info("Пользователь id={} обновил комментарий id={}", userId, commentId);
        return CommentMapper.toCommentResponseDto(comment);

    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = getCommentById(commentId);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Только автор может удалить комментарий");
        }

        commentRepository.delete(comment);
        log.info("Пользователь id={} удалил комментарий id={}", userId, commentId);
    }

    public List<CommentResponseDto> getUserComments(Long userId, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("createdDate").descending());

        checkUserExists(userId);

        List<Comment> comments = commentRepository.findByAuthorId(userId, pageable);

        log.info("Получение комментариев пользователя id={}", userId);
        return comments.stream()
                .map(CommentMapper::toCommentResponseDto)
                .collect(Collectors.toList());

    }

    public List<CommentResponseDto> getEventComments(Long eventId, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("createdDate").descending());

        checkEventExists(eventId);

        List<Comment> comments = commentRepository.findByEventId(eventId, pageable);

        log.info("Получение комментариев для события id={}", eventId);

        return comments.stream()
                .map(CommentMapper::toCommentResponseDto)
                .collect(Collectors.toList());
    }

    public CommentResponseDto getComment(Long commentId) {
        Comment comment = getCommentById(commentId);

        log.info("Получение комментариев c id={}", commentId);

        return CommentMapper.toCommentResponseDto(comment);
    }

    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        Comment comment = getCommentById(commentId);
        commentRepository.delete(comment);
        log.info("Админ удалил комментарий c id={}", commentId);
    }

    private Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не найден"));
    }

    private void checkUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
    }

    private void checkEventExists(Long eventID) {
        if (!eventRepository.existsById(eventID)) {
            throw new NotFoundException("Событие с id = " + eventID + " не найдено");
        }
    }

}
