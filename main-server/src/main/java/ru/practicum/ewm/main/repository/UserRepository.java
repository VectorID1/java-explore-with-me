package ru.practicum.ewm.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.main.model.User;


import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u " +
            "WHERE (:ids IS NULL OR u.id IN :ids) " +
            "ORDER BY u.id")
    List<User> findUsersWithFilter(@Param("ids") List<Long> ids);
}
