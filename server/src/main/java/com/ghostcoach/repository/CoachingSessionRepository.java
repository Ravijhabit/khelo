package com.ghostcoach.repository;

import com.ghostcoach.model.CoachingSession;
import com.ghostcoach.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CoachingSessionRepository extends JpaRepository<CoachingSession, Long> {
    List<CoachingSession> findByUserOrderByCreatedAtDesc(User user);
    Optional<CoachingSession> findByIdAndUser(Long id, User user);
}
