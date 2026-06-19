package com.smp.conversation;

import java.util.List;
import java.util.UUID;

import com.smp.conversation.dto.ConversationMessageResponseDto;
import com.smp.conversation.dto.ConversationPartnerDto;
import com.smp.conversation.dto.ConversationResponseDto;
import com.smp.conversation.dto.CreateConversationRequestDto;
import com.smp.conversation.dto.SendConversationMessageRequestDto;
import com.smp.patient.dto.PatientDocumentResponseDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping("/partners")
    public List<ConversationPartnerDto> getAvailablePartners(Authentication authentication) {
        String email = requireEmail(authentication);

        if (hasAuthority(authentication, "ROLE_DOCTOR")) {
            UUID currentDoctorId = conversationService.requireActiveDoctorByEmail(email).getId();
            return conversationService.getDoctorPartners(currentDoctorId);
        }

        if (hasAuthority(authentication, "ROLE_PATIENT")) {
            UUID currentPatientId = conversationService.requireEnabledPatientByEmail(email).getId();
            return conversationService.getPatientPartners(currentPatientId);
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only doctor and patient can list conversation partners");
    }

    @PostMapping("/threads")
    public ConversationResponseDto createOrGetConversation(
            Authentication authentication,
            @Valid @RequestBody CreateConversationRequestDto request) {
        String email = requireEmail(authentication);

        if (hasAuthority(authentication, "ROLE_DOCTOR")) {
            UUID currentDoctorId = conversationService.requireActiveDoctorByEmail(email).getId();
            if (!currentDoctorId.equals(request.doctorId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor can only create own conversations");
            }
        } else if (hasAuthority(authentication, "ROLE_PATIENT")) {
            UUID currentPatientId = conversationService.requireEnabledPatientByEmail(email).getId();
            if (!currentPatientId.equals(request.patientId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Patient can only create own conversations");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only doctor and patient can create conversations");
        }

        return conversationService.createOrGetConversation(request.doctorId(), request.patientId());
    }

    @GetMapping("/threads/{conversationId}/messages")
    public List<ConversationMessageResponseDto> getMessages(
            @PathVariable UUID conversationId,
            Authentication authentication) {
        ConversationDao conversation = conversationService.getConversationForParticipant(conversationId, requireEmail(authentication));
        return conversationService.getMessages(conversation.getId());
    }

    @PostMapping("/threads/{conversationId}/messages")
    public ConversationMessageResponseDto sendTextMessage(
            @PathVariable UUID conversationId,
            Authentication authentication,
            @Valid @RequestBody SendConversationMessageRequestDto request) {
        String email = requireEmail(authentication);
        ConversationDao conversation = conversationService.getConversationForParticipant(conversationId, email);
        ConversationSenderRole senderRole = conversationService.resolveSenderRole(conversation, email);

        return conversationService.sendTextMessage(
                conversation,
                senderRole,
                conversationService.resolveSenderName(conversation, senderRole),
                request.content());
    }

    @PostMapping(value = "/threads/{conversationId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ConversationMessageResponseDto sendAttachmentMessage(
            @PathVariable UUID conversationId,
            Authentication authentication,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "content", required = false) String content) {
        String email = requireEmail(authentication);
        ConversationDao conversation = conversationService.getConversationForParticipant(conversationId, email);
        ConversationSenderRole senderRole = conversationService.resolveSenderRole(conversation, email);

        return conversationService.sendAttachmentMessage(
                conversation,
                senderRole,
                conversationService.resolveSenderName(conversation, senderRole),
                content,
                file);
    }

    @PostMapping("/threads/{conversationId}/attachments/{attachmentId}/save-to-patient-profile")
    public PatientDocumentResponseDto saveAttachmentToPatientProfile(
            @PathVariable UUID conversationId,
            @PathVariable UUID attachmentId,
            Authentication authentication,
            @RequestParam(value = "description", required = false) String description) {
        String email = requireEmail(authentication);
        ConversationDao conversation = conversationService.getConversationForParticipant(conversationId, email);

        if (!conversation.getDoctor().getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only doctor can save to patient profile");
        }

        return conversationService.saveAttachmentToPatientProfile(conversation, attachmentId, description);
    }

    private String requireEmail(Authentication authentication) {
        String email = authentication == null ? null : authentication.getName();
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return email;
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null
                && authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                        .anyMatch(authority::equals);
    }
}
