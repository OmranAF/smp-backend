package com.smp.prescription;

import java.util.List;
import java.util.UUID;

import com.smp.appointment.AppointmentDao;
import com.smp.appointment.AppointmentRepository;
import com.smp.prescription.dto.DoctorPrescriptionRequestDto;
import com.smp.prescription.dto.DoctorPrescriptionResponseDto;
import com.smp.prescription.dto.PatientPrescriptionResponseDto;
import com.smp.prescription.dto.PrescriptionTicketDetailsDto;
import com.smp.user.DoctorDao;
import com.smp.user.DoctorRepository;
import com.smp.user.PharmacistDao;
import com.smp.user.PharmacistRepository;
import com.smp.user.PatientDao;
import com.smp.user.PatientRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final DoctorRepository doctorRepository;
    private final PharmacistRepository pharmacistRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public DoctorPrescriptionResponseDto createPrescription(UUID doctorId, DoctorPrescriptionRequestDto request) {
        DoctorDao doctor = requireActiveDoctor(doctorId);

        PatientDao patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));

        AppointmentDao appointment = null;
        if (request.appointmentId() != null) {
            appointment = appointmentRepository.findById(request.appointmentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

            boolean doctorMismatch = appointment.getDoctor() == null
                    || appointment.getDoctor().getId() == null
                    || !doctorId.equals(appointment.getDoctor().getId());
            boolean patientMismatch = appointment.getPatient() == null
                    || appointment.getPatient().getId() == null
                    || !patient.getId().equals(appointment.getPatient().getId());

            if (doctorMismatch || patientMismatch) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment does not belong to this doctor and patient");
            }
        }

        PrescriptionDao prescription = new PrescriptionDao();
        prescription.setDoctor(doctor);
        prescription.setPatient(patient);
        prescription.setAppointment(appointment);
        prescription.setMedicationName(request.medicationName().trim());
        prescription.setDosage(request.dosage().trim());
        prescription.setInstructions(request.instructions().trim());
        prescription.setValidUntil(request.validUntil());
        prescription.setTicketId(UUID.randomUUID());
        prescription.setAccessToken(UUID.randomUUID().toString());

        return toResponse(prescriptionRepository.save(prescription));
    }

    @Transactional(readOnly = true)
    public List<DoctorPrescriptionResponseDto> getDoctorPrescriptions(UUID doctorId) {
        requireActiveDoctor(doctorId);

        return prescriptionRepository.findByDoctor_IdOrderByIssuedAtDesc(doctorId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PatientPrescriptionResponseDto> getPatientPrescriptions(UUID patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        }

        return prescriptionRepository.findByPatient_IdOrderByIssuedAtDesc(patientId).stream()
                .map(this::toPatientResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PrescriptionTicketDetailsDto getTicketDetails(UUID ticketId, String token) {
        PrescriptionDao prescription = getActiveTicketPrescription(ticketId, token);

        return new PrescriptionTicketDetailsDto(
                prescription.getTicketId(),
                prescription.getMedicationName(),
                prescription.getDosage(),
                prescription.getInstructions(),
                prescription.getIssuedAt(),
                prescription.getValidUntil(),
                prescription.getDoctor().getName(),
                prescription.getPatient().getName());
    }

    @Transactional(readOnly = true)
    public String buildQrPayloadUrl(PrescriptionDao prescription) {
        String ticketUrl = buildAbsoluteOrRelative(
            "/api/prescriptions/tickets/{ticketId}",
            prescription.getTicketId(),
            prescription.getAccessToken());

        return String.join(System.lineSeparator(),
                "SMP DIGITAL PRESCRIPTION",
                "Doctor: " + prescription.getDoctor().getName(),
                "Patient: " + prescription.getPatient().getName(),
                "Medication: " + prescription.getMedicationName(),
                "Status: " + (prescription.getFulfillmentStatus() == null ? PrescriptionFulfillmentStatus.PENDING.name() : prescription.getFulfillmentStatus().name()),
                "Ticket ID: " + prescription.getTicketId(),
                "Token: " + prescription.getAccessToken(),
                "Ticket URL: " + ticketUrl);
    }

    @Transactional(readOnly = true)
    public PrescriptionDao findTicketPrescription(UUID ticketId, String token) {
        return getActiveTicketPrescription(ticketId, token);
    }

    @Transactional
    public PatientPrescriptionResponseDto dispensePrescriptionByTicket(
            UUID pharmacistId,
            UUID ticketId,
            String token,
            PrescriptionFulfillmentStatus status,
            String note) {
        if (status == PrescriptionFulfillmentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dispense status must be APPROVED or FINISHED");
        }

        PharmacistDao pharmacist = pharmacistRepository.findById(pharmacistId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pharmacist not found"));

        PrescriptionDao prescription = getActiveTicketPrescription(ticketId, token);
        prescription.setFulfillmentStatus(status);
        prescription.setDispensedByPharmacist(pharmacist);
        prescription.setDispensedAt(java.time.LocalDateTime.now());

        String normalizedNote = note == null ? null : note.trim();
        prescription.setDispenseNote(normalizedNote == null || normalizedNote.isEmpty() ? null : normalizedNote);

        // Consume QR token after dispense so it is no longer shown/usable.
        prescription.setAccessToken("");

        return toPatientResponse(prescriptionRepository.save(prescription));
    }

    private DoctorPrescriptionResponseDto toResponse(PrescriptionDao prescription) {
        String qrCodeImageUrl = buildAbsoluteOrRelative(
            "/api/prescriptions/tickets/{ticketId}/qr",
            prescription.getTicketId(),
            prescription.getAccessToken());

        return new DoctorPrescriptionResponseDto(
                prescription.getId(),
                prescription.getDoctor().getId(),
                prescription.getDoctor().getName(),
                prescription.getPatient().getId(),
                prescription.getPatient().getName(),
                prescription.getAppointment() == null ? null : prescription.getAppointment().getId(),
            prescription.getTicketId(),
            qrCodeImageUrl,
                prescription.getMedicationName(),
                prescription.getDosage(),
                prescription.getInstructions(),
                prescription.getFulfillmentStatus() == null ? null : prescription.getFulfillmentStatus().name(),
                prescription.getIssuedAt(),
                prescription.getValidUntil());
    }

        private PatientPrescriptionResponseDto toPatientResponse(PrescriptionDao prescription) {
            String qrCodeImageUrl = buildAbsoluteOrRelative(
                "/api/prescriptions/tickets/{ticketId}/qr",
                prescription.getTicketId(),
                prescription.getAccessToken());

            String detailsUrl = buildAbsoluteOrRelative(
                "/api/prescriptions/tickets/{ticketId}",
                prescription.getTicketId(),
                prescription.getAccessToken());

        return new PatientPrescriptionResponseDto(
            prescription.getId(),
            prescription.getDoctor().getId(),
            prescription.getDoctor().getName(),
            prescription.getPatient().getId(),
            prescription.getPatient().getName(),
            prescription.getAppointment() == null ? null : prescription.getAppointment().getId(),
            prescription.getTicketId(),
            canUseTicket(prescription) ? prescription.getAccessToken() : null,
            canUseTicket(prescription) ? qrCodeImageUrl : null,
            canUseTicket(prescription) ? detailsUrl : null,
            prescription.getMedicationName(),
            prescription.getDosage(),
            prescription.getInstructions(),
            prescription.getFulfillmentStatus() == null ? null : prescription.getFulfillmentStatus().name(),
            prescription.getDispensedByPharmacist() == null ? null : prescription.getDispensedByPharmacist().getId(),
            prescription.getDispensedByPharmacist() == null ? null : prescription.getDispensedByPharmacist().getName(),
            prescription.getDispensedAt(),
            prescription.getDispenseNote(),
            prescription.getIssuedAt(),
            prescription.getValidUntil());
        }

    private String buildAbsoluteOrRelative(String pathTemplate, UUID ticketId, String token) {
        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(pathTemplate)
                    .queryParam("token", token)
                    .buildAndExpand(ticketId)
                    .toUriString();
        } catch (IllegalStateException ex) {
            return pathTemplate
                    .replace("{ticketId}", ticketId.toString())
                    + "?token=" + token;
        }
    }

    private DoctorDao requireActiveDoctor(UUID doctorId) {
        DoctorDao doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));
        if (doctor.getUser() == null || !doctor.getUser().isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor account is disabled by admin");
        }
        if (!doctor.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor account is waiting for admin activation");
        }
        return doctor;
    }

    private PrescriptionDao getActiveTicketPrescription(UUID ticketId, String token) {
        PrescriptionDao prescription = prescriptionRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription ticket not found"));

        if (!canUseTicket(prescription)) {
            throw new ResponseStatusException(HttpStatus.GONE, "Prescription QR ticket already used");
        }

        if (token == null || token.isBlank() || !token.equals(prescription.getAccessToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid prescription token");
        }

        return prescription;
    }

    private boolean canUseTicket(PrescriptionDao prescription) {
        return prescription.getAccessToken() != null && !prescription.getAccessToken().isBlank();
    }
}
