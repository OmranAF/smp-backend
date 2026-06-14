package com.smp.patient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.smp.patient.dto.PatientDocumentResponseDto;
import com.smp.patient.dto.PatientHealthStatusRequestDto;
import com.smp.patient.dto.PatientHealthStatusResponseDto;
import com.smp.user.PatientDao;
import com.smp.user.PatientRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientCareController {

    private final PatientRepository patientRepository;
    private final AllergyRepository allergyRepository;
    private final PatientDocumentRepository patientDocumentRepository;
    private final PatientHealthStatusRepository patientHealthStatusRepository;

    @PutMapping("/{patientId}/health-status")
    @Transactional
    public PatientHealthStatusResponseDto updateHealthStatus(
            @PathVariable UUID patientId,
            @Valid @RequestBody PatientHealthStatusRequestDto request) {
        PatientDao patient = findPatient(patientId);
        PatientHealthStatusDao status = patient.getHealthStatus();
        if (status == null) {
            status = new PatientHealthStatusDao();
            status.setPatient(patient);
        }
        status.setStatus(request.status());
        status.setNotes(request.notes());
        status = patientHealthStatusRepository.save(status);
        patient.setHealthStatus(status);
        patientRepository.save(patient);
        return toHealthStatusDto(status);
    }

    @PostMapping("/{patientId}/allergies")
    @Transactional
    public ResponseEntity<Void> setAllergies(@PathVariable UUID patientId, @RequestBody List<UUID> allergyIds) {
        PatientDao patient = findPatient(patientId);
        Set<AllergyDao> allergies = new HashSet<>(allergyRepository.findAllById(allergyIds));
        patient.setAllergies(allergies);
        patientRepository.save(patient);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{patientId}/documents")
    public List<PatientDocumentResponseDto> getDocuments(@PathVariable UUID patientId) {
        PatientDao patient = findPatient(patientId);
        return patientDocumentRepository.findAll().stream()
                .filter(document -> document.getPatient() != null && patientId.equals(document.getPatient().getId()))
                .map(this::toDocumentDto)
                .toList();
    }

    @PostMapping(value = "/{patientId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public List<PatientDocumentResponseDto> uploadDocuments(
            @PathVariable UUID patientId,
            @RequestPart("files") MultipartFile[] files,
            @RequestParam(value = "description", required = false) String description) throws IOException {
        PatientDao patient = findPatient(patientId);
        Path uploadDir = Path.of("uploads", "patients", patientId.toString());
        Files.createDirectories(uploadDir);

        List<PatientDocumentResponseDto> result = java.util.Arrays.stream(files)
                .map(file -> {
                    try {
                        Path target = uploadDir.resolve(file.getOriginalFilename());
                        Files.write(target, file.getBytes());

                        PatientDocumentDao document = new PatientDocumentDao();
                        document.setPatient(patient);
                        document.setFileName(file.getOriginalFilename());
                        document.setFilePath(target.toString());
                        document.setContentType(file.getContentType());
                        document.setFileSize(file.getSize());
                        document.setDescription(description);
                        return toDocumentDto(patientDocumentRepository.save(document));
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                })
                .toList();

        return result;
    }

    private PatientDao findPatient(UUID patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
    }

    private PatientHealthStatusResponseDto toHealthStatusDto(PatientHealthStatusDao status) {
        return new PatientHealthStatusResponseDto(
                status.getId(),
                status.getPatient().getId(),
                status.getStatus(),
                status.getNotes(),
                status.getCreatedAt(),
                status.getUpdatedAt());
    }

    private PatientDocumentResponseDto toDocumentDto(PatientDocumentDao document) {
        return new PatientDocumentResponseDto(
                document.getId(),
                document.getFileName(),
                document.getContentType(),
                document.getFileSize(),
                document.getDescription(),
                document.getUploadedAt());
    }
}