package ru.practicum.ewm.main.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    //    @Query("SELECT e FROM Event e " +
//            "WHERE (:users IS NULL OR e.initiator.id IN :users) " +
//            "AND (:states IS NULL OR e.state IN :states) " +
//            "AND (:categories IS NULL OR e.category.id IN :categories) " +
//            "AND (:rangeStart IS NULL OR e.eventDate >= :rangeStart) " +
//            "AND (:rangeEnd IS NULL OR e.eventDate <= :rangeEnd)")
    @Query("SELECT e FROM Event e WHERE " +
            "(:users IS NULL OR e.initiator.id IN :users) " +
            "AND (:states IS NULL OR e.state IN :states) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND (cast(:rangeStart as org.hibernate.type.TimestampType) IS NULL OR e.eventDate >= :rangeStart) " +
            "AND (cast(:rangeEnd as org.hibernate.type.TimestampType) IS NULL OR e.eventDate <= :rangeEnd)")
    List<Event> findEventsByAdminFilters(
            @Param("users") List<Long> users,
            @Param("states") List<EventState> states,
            @Param("categories") List<Long> categories,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable);

    List<Event> findByInitiatorId(Long userId, Pageable pageable);

    //    @Query("SELECT e FROM Event e " +
//            "WHERE e.state = 'PUBLISHED' " +
//            "AND (:text IS NULL OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) " +
//            "     OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) " +
//            "AND (:categories IS NULL OR e.category.id IN :categories) " +
//            "AND (:paid IS NULL OR e.paid = :paid) " +
//            "AND (:rangeStart IS NULL OR e.eventDate >= :rangeStart) " +
//            "AND (:rangeEnd IS NULL OR e.eventDate <= :rangeEnd) " +
//            "AND (:onlyAvailable IS NULL OR :onlyAvailable = false OR " +
//            "     e.participantLimit = 0 OR e.participantLimit > " +
//            "     (SELECT COUNT(r) FROM Request r WHERE r.event = e AND r.status = 'CONFIRMED'))")
//    List<Event> findPublicEvents(
//            @Param("text") String text,
//            @Param("categories") List<Long> categories,
//            @Param("paid") Boolean paid,
//            @Param("rangeStart") LocalDateTime rangeStart,
//            @Param("rangeEnd") LocalDateTime rangeEnd,
//            @Param("onlyAvailable") Boolean onlyAvailable,
//            Pageable pageable);
    @Query("SELECT e FROM Event e WHERE " +
            "e.state = 'PUBLISHED' AND " +
            "(cast(:text as string) IS NULL OR " +
            " lower(cast(e.annotation as string)) like lower(concat('%', cast(:text as string), '%')) OR " +
            " lower(cast(e.description as string)) like lower(concat('%', cast(:text as string), '%'))) AND " +
            "(cast(:categories as string) IS NULL OR e.category.id IN :categories) AND " +
            "(cast(:paid as string) IS NULL OR e.paid = :paid) AND " +
            "(cast(:rangeStart as string) IS NULL OR e.eventDate >= :rangeStart) AND " +
            "(cast(:rangeEnd as string) IS NULL OR e.eventDate <= :rangeEnd)")
    List<Event> findPublicEvents(
            @Param("text") String text,
            @Param("categories") List<Long> categories,
            @Param("paid") Boolean paid,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable);

    List<Event> findAllByIdIn(List<Long> ids);
}

