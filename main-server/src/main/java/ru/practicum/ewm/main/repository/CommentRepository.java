package ru.practicum.ewm.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.main.model.Comment;
import org.springframework.data.domain.Pageable;


import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByAuthorId(Long userId, Pageable pageable);

    List<Comment> findByEventId(Long eventId, Pageable pageable);

}
