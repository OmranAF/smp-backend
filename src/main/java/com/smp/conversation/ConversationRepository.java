package com.smp.conversation;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<ConversationDao, UUID> {

    Optional<ConversationDao> findByDoctor_IdAndPatient_Id(UUID doctorId, UUID patientId);
}
