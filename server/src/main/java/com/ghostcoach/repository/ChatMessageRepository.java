package com.ghostcoach.repository;

import com.ghostcoach.model.ChatMessage;
import com.ghostcoach.model.CoachingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionOrderByCreatedAtAsc(CoachingSession session);
}
