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
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public DoctorPrescriptionResponseDto createPrescription(UUID doctorId, DoctorPrescriptionRequestDto request) {
        DoctorDao doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));

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
        if (!doctorRepository.existsById(doctorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found");
        }

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
        PrescriptionDao prescription = prescriptionRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription ticket not found"));

        if (token == null || token.isBlank() || !token.equals(prescription.getAccessToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid prescription token");
        }

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
        return buildAbsoluteOrRelative(
            "/api/prescriptions/tickets/{ticketId}",
            prescription.getTicketId(),
            prescription.getAccessToken());
    }

    @Transactional(readOnly = true)
    public PrescriptionDao findTicketPrescription(UUID ticketId, String token) {
        PrescriptionDao prescription = prescriptionRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription ticket not found"));

        if (token == null || token.isBlank() || !token.equals(prescription.getAccessToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid prescription token");
        }

        return prescription;
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
            prescription.getAccessToken(),
            qrCodeImageUrl,
            detailsUrl,
            prescription.getMedicationName(),
            prescription.getDosage(),
            prescription.getInstructions(),
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
}
