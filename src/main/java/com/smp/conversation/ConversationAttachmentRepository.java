package com.smp.conversation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationAttachmentRepository extends JpaRepository<ConversationAttachmentDao, UUID> {

    List<ConversationAttachmentDao> findByMessage_IdIn(List<UUID> messageIds);

    List<ConversationAttachmentDao> findByMessage_CreatedAtBefore(LocalDateTime cutoff);

    void deleteByMessage_CreatedAtBefore(LocalDateTime cutoff);

    Optional<ConversationAttachmentDao> findByIdAndMessage_Conversation_Id(UUID id, UUID conversationId);
}
