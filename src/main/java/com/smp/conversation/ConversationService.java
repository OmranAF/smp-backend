package com.smp.conversation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.smp.appointment.AppointmentRepository;
import com.smp.conversation.dto.ConversationAttachmentResponseDto;
import com.smp.conversation.dto.ConversationMessageResponseDto;
import com.smp.conversation.dto.ConversationPartnerDto;
import com.smp.conversation.dto.ConversationResponseDto;
import com.smp.patient.PatientDocumentDao;
import com.smp.patient.PatientDocumentRepository;
import com.smp.patient.dto.PatientDocumentResponseDto;
import com.smp.user.DoctorDao;
import com.smp.user.DoctorRepository;
import com.smp.user.PatientDao;
import com.smp.user.PatientRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ConversationAttachmentRepository attachmentRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final PatientDocumentRepository patientDocumentRepository;

    @Transactional
    public ConversationResponseDto createOrGetConversation(UUID doctorId, UUID patientId) {
        DoctorDao doctor = requireActiveDoctor(doctorId);
        PatientDao patient = requireEnabledPatient(patientId);
        ensureDoctorPatientRelationship(doctorId, patientId);

        ConversationDao conversation = conversationRepository.findByDoctor_IdAndPatient_Id(doctorId, patientId)
                .orElseGet(() -> {
                    ConversationDao created = new ConversationDao();
                    created.setDoctor(doctor);
                    created.setPatient(patient);
                    return conversationRepository.save(created);
                });

        return toConversationDto(conversation);
    }

        @Transactional(readOnly = true)
        public List<ConversationPartnerDto> getDoctorPartners(UUID doctorId) {
        requireActiveDoctor(doctorId);
        return appointmentRepository.findDistinctPatientsByDoctorId(doctorId).stream()
            .filter(patient -> patient.getUser() != null && patient.getUser().isEnabled())
            .map(patient -> new ConversationPartnerDto(
                patient.getId(),
                patient.getName(),
                patient.getUser().getEmail(),
                "Patient"))
            .toList();
        }

        @Transactional(readOnly = true)
        public List<ConversationPartnerDto> getPatientPartners(UUID patientId) {
        requireEnabledPatient(patientId);
        return appointmentRepository.findDistinctDoctorsByPatientId(patientId).stream()
            .filter(doctor -> doctor.getUser() != null && doctor.getUser().isEnabled() && doctor.isActive())
            .map(doctor -> new ConversationPartnerDto(
                doctor.getId(),
                doctor.getName(),
                doctor.getUser().getEmail(),
                doctor.getSpecialization()))
            .toList();
        }

    @Transactional(readOnly = true)
    public ConversationDao getConversationForParticipant(UUID conversationId, String participantEmail) {
        ConversationDao conversation = conversationRepository.findById(conversationId)
                .orElseThrow(notFound("Conversation not found"));

        ensureConversationStillActive(conversation);

        boolean doctorMatch = conversation.getDoctor() != null
                && conversation.getDoctor().getUser() != null
                && participantEmail.equalsIgnoreCase(conversation.getDoctor().getUser().getEmail());

        boolean patientMatch = conversation.getPatient() != null
                && conversation.getPatient().getUser() != null
                && participantEmail.equalsIgnoreCase(conversation.getPatient().getUser().getEmail());

        if (!doctorMatch && !patientMatch) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own conversation");
        }

        return conversation;
    }

    @Transactional(readOnly = true)
    public List<ConversationMessageResponseDto> getMessages(UUID conversationId) {
        List<ConversationMessageDao> messages = messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId);

        if (messages.isEmpty()) {
            return List.of();
        }

        List<UUID> messageIds = messages.stream().map(ConversationMessageDao::getId).toList();
        Map<UUID, List<ConversationAttachmentDao>> attachmentByMessageId = attachmentRepository.findByMessage_IdIn(messageIds)
                .stream()
                .collect(Collectors.groupingBy(attachment -> attachment.getMessage().getId()));

        return messages.stream()
                .map(message -> toMessageDto(message, attachmentByMessageId.getOrDefault(message.getId(), List.of())))
                .toList();
    }

    @Transactional
    public ConversationMessageResponseDto sendTextMessage(
            ConversationDao conversation,
            ConversationSenderRole senderRole,
            String senderName,
            String content) {
        ConversationMessageDao message = new ConversationMessageDao();
        message.setConversation(conversation);
        message.setSenderRole(senderRole);
        message.setSenderName(senderName);
        message.setContent(content == null ? null : content.trim());

        ConversationMessageDao saved = messageRepository.save(message);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return toMessageDto(saved, List.of());
    }

    @Transactional
    public ConversationMessageResponseDto sendAttachmentMessage(
            ConversationDao conversation,
            ConversationSenderRole senderRole,
            String senderName,
            String content,
            MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attachment file is required");
        }

        ConversationMessageDao message = new ConversationMessageDao();
        message.setConversation(conversation);
        message.setSenderRole(senderRole);
        message.setSenderName(senderName);
        message.setContent(content == null ? null : content.trim());
        ConversationMessageDao savedMessage = messageRepository.save(message);

        String safeFileName = safeFileName(file.getOriginalFilename());
        Path uploadDir = Path.of("uploads", "conversations", conversation.getId().toString(), savedMessage.getId().toString());
        try {
            Files.createDirectories(uploadDir);
            Path targetPath = uploadDir.resolve(safeFileName);
            Files.write(targetPath, file.getBytes());

            ConversationAttachmentDao attachment = new ConversationAttachmentDao();
            attachment.setMessage(savedMessage);
            attachment.setFileName(safeFileName);
            attachment.setFilePath(targetPath.toString());
            attachment.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setSavedToPatientProfile(false);
            ConversationAttachmentDao savedAttachment = attachmentRepository.save(attachment);

            conversation.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(conversation);

            return toMessageDto(savedMessage, List.of(savedAttachment));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store attachment");
        }
    }

    @Transactional
    public PatientDocumentResponseDto saveAttachmentToPatientProfile(
            ConversationDao conversation,
            UUID attachmentId,
            String description) {
        ConversationAttachmentDao attachment = attachmentRepository
                .findByIdAndMessage_Conversation_Id(attachmentId, conversation.getId())
                .orElseThrow(notFound("Attachment not found"));

        Path sourcePath = Path.of(attachment.getFilePath());
        if (!Files.exists(sourcePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment file not found on server");
        }

        UUID patientId = conversation.getPatient().getId();
        String targetName = "chat-" + attachment.getId() + "-" + attachment.getFileName();
        Path patientDir = Path.of("uploads", "patients", patientId.toString());

        try {
            Files.createDirectories(patientDir);
            Path targetPath = patientDir.resolve(targetName);
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            PatientDocumentDao document = new PatientDocumentDao();
            document.setPatient(conversation.getPatient());
            document.setFileName(targetName);
            document.setFilePath(targetPath.toString());
            document.setContentType(attachment.getContentType());
            document.setFileSize(attachment.getFileSize());
            document.setDescription(StringUtils.hasText(description)
                    ? description.trim()
                    : "Saved from doctor-patient conversation");
            PatientDocumentDao savedDocument = patientDocumentRepository.save(document);

            attachment.setSavedToPatientProfile(true);
            attachmentRepository.save(attachment);

            return new PatientDocumentResponseDto(
                    savedDocument.getId(),
                    savedDocument.getFileName(),
                    savedDocument.getContentType(),
                    savedDocument.getFileSize(),
                    savedDocument.getDescription(),
                    savedDocument.getUploadedAt());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save file to patient profile");
        }
    }

    public DoctorDao requireActiveDoctor(UUID doctorId) {
        DoctorDao doctor = doctorRepository.findById(doctorId)
                .orElseThrow(notFound("Doctor not found"));

        if (doctor.getUser() == null || !doctor.getUser().isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor account is disabled by admin");
        }

        if (!doctor.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor account is waiting for admin activation");
        }

        return doctor;
    }

    public DoctorDao requireActiveDoctorByEmail(String email) {
        DoctorDao doctor = doctorRepository.findByUser_Email(email.trim().toLowerCase())
                .orElseThrow(notFound("Doctor not found"));
        return requireActiveDoctor(doctor.getId());
    }

    public PatientDao requireEnabledPatient(UUID patientId) {
        PatientDao patient = patientRepository.findById(patientId)
                .orElseThrow(notFound("Patient not found"));

        if (patient.getUser() == null || !patient.getUser().isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Patient account is disabled by admin");
        }

        return patient;
    }

    public PatientDao requireEnabledPatientByEmail(String email) {
        PatientDao patient = patientRepository.findByUser_Email(email.trim().toLowerCase())
                .orElseThrow(notFound("Patient not found"));
        return requireEnabledPatient(patient.getId());
    }

    public void ensureDoctorPatientRelationship(UUID doctorId, UUID patientId) {
        if (!appointmentRepository.existsByDoctor_IdAndPatient_Id(doctorId, patientId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Conversation is only allowed when patient has a profile with this doctor");
        }
    }

    public ConversationSenderRole resolveSenderRole(ConversationDao conversation, String participantEmail) {
        if (conversation.getDoctor().getUser().getEmail().equalsIgnoreCase(participantEmail)) {
            return ConversationSenderRole.DOCTOR;
        }
        if (conversation.getPatient().getUser().getEmail().equalsIgnoreCase(participantEmail)) {
            return ConversationSenderRole.PATIENT;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only send messages in your own conversation");
    }

    public String resolveSenderName(ConversationDao conversation, ConversationSenderRole role) {
        return role == ConversationSenderRole.DOCTOR
                ? conversation.getDoctor().getName()
                : conversation.getPatient().getName();
    }

    private void ensureConversationStillActive(ConversationDao conversation) {
        requireActiveDoctor(conversation.getDoctor().getId());
        requireEnabledPatient(conversation.getPatient().getId());
        ensureDoctorPatientRelationship(conversation.getDoctor().getId(), conversation.getPatient().getId());
    }

    private ConversationResponseDto toConversationDto(ConversationDao conversation) {
        return new ConversationResponseDto(
                conversation.getId(),
                conversation.getDoctor().getId(),
                conversation.getDoctor().getName(),
                conversation.getPatient().getId(),
                conversation.getPatient().getName(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt());
    }

    private ConversationMessageResponseDto toMessageDto(
            ConversationMessageDao message,
            List<ConversationAttachmentDao> attachments) {
        List<ConversationAttachmentResponseDto> attachmentDtos = attachments == null
                ? Collections.emptyList()
                : attachments.stream().map(this::toAttachmentDto).toList();

        return new ConversationMessageResponseDto(
                message.getId(),
                message.getSenderRole(),
                message.getSenderName(),
                message.getContent(),
                message.getCreatedAt(),
                attachmentDtos);
    }

    private ConversationAttachmentResponseDto toAttachmentDto(ConversationAttachmentDao attachment) {
        return new ConversationAttachmentResponseDto(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getUploadedAt(),
                attachment.isSavedToPatientProfile());
    }

    private String safeFileName(String original) {
        String candidate = StringUtils.cleanPath(original == null ? "attachment.bin" : original);
        if (!StringUtils.hasText(candidate)) {
            return "attachment.bin";
        }

        Path normalized = Path.of(candidate).getFileName();
        return normalized == null ? "attachment.bin" : normalized.toString();
    }

    private Supplier<ResponseStatusException> notFound(String message) {
        return () -> new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
