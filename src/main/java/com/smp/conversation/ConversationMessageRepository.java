package com.smp.conversation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessageDao, UUID> {

    List<ConversationMessageDao> findByConversation_IdOrderByCreatedAtAsc(UUID conversationId);

    List<ConversationMessageDao> findByCreatedAtBefore(LocalDateTime cutoff);

    void deleteByCreatedAtBefore(LocalDateTime cutoff);
}
