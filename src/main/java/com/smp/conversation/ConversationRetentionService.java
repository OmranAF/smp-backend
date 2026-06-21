package com.smp.conversation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationRetentionService {

    private static final Logger log = LoggerFactory.getLogger(ConversationRetentionService.class);

    private final ConversationMessageRepository messageRepository;
    private final ConversationAttachmentRepository attachmentRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeMessagesOlderThanSixtyHours() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(60);

        List<ConversationAttachmentDao> oldAttachments = attachmentRepository.findByMessage_CreatedAtBefore(cutoff);
        for (ConversationAttachmentDao attachment : oldAttachments) {
            String filePath = attachment.getFilePath();
            if (filePath == null || filePath.isBlank()) {
                continue;
            }

            try {
                Files.deleteIfExists(Path.of(filePath));
            } catch (IOException exception) {
                log.warn("Failed to delete attachment file {}", filePath, exception);
            }
        }

        if (!oldAttachments.isEmpty()) {
            attachmentRepository.deleteByMessage_CreatedAtBefore(cutoff);
        }

        List<ConversationMessageDao> oldMessages = messageRepository.findByCreatedAtBefore(cutoff);
        if (!oldMessages.isEmpty()) {
            messageRepository.deleteByCreatedAtBefore(cutoff);
            log.info("Deleted {} conversation messages and {} attachments older than {}", oldMessages.size(), oldAttachments.size(), cutoff);
        }
    }
}