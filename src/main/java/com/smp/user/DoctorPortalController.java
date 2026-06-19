package com.smp.user;

import java.util.List;
import java.util.UUID;

import com.smp.appointment.AppointmentService;
import com.smp.appointment.AppointmentRepository;
import com.smp.appointment.dto.AppointmentResponseDto;
import com.smp.prescription.PrescriptionService;
import com.smp.prescription.dto.DoctorPrescriptionRequestDto;
import com.smp.prescription.dto.DoctorPrescriptionResponseDto;
import com.smp.user.dto.DoctorAppointmentCancelRequestDto;
import com.smp.user.dto.DoctorLoginRequestDto;
import com.smp.user.dto.DoctorLoginResponseDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorPortalController {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;
    private final PrescriptionService prescriptionService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public DoctorLoginResponseDto login(@Valid @RequestBody DoctorLoginRequestDto request) {
        DoctorDao doctor = doctorRepository.findByUser_Email(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), doctor.getUser().getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (!doctor.getUser().isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor account is disabled by admin");
        }

        if (!doctor.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor account is waiting for admin activation");
        }

        return new DoctorLoginResponseDto(
                doctor.getId(),
                doctor.getName(),
                doctor.getUser().getEmail(),
                doctor.getSpecialization());
    }

    @GetMapping("/{doctorId}/appointments")
    public List<AppointmentResponseDto> getDoctorAppointments(@PathVariable UUID doctorId) {
        requireActiveDoctor(doctorId);

        return appointmentRepository.findByDoctor_IdOrderByAppointmentTimeDesc(doctorId).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/{doctorId}/appointments/{appointmentId}/cancel")
    public AppointmentResponseDto cancelDoctorAppointment(
            @PathVariable UUID doctorId,
            @PathVariable UUID appointmentId,
            @Valid @RequestBody DoctorAppointmentCancelRequestDto request) {
        requireActiveDoctor(doctorId);

        return appointmentService.cancelByDoctor(doctorId, appointmentId, request.cancellationReason());
    }

    @PostMapping("/{doctorId}/prescriptions")
    public DoctorPrescriptionResponseDto createPrescription(
            @PathVariable UUID doctorId,
            @Valid @RequestBody DoctorPrescriptionRequestDto request) {
        requireActiveDoctor(doctorId);
        return prescriptionService.createPrescription(doctorId, request);
    }

    @GetMapping("/{doctorId}/prescriptions")
    public List<DoctorPrescriptionResponseDto> getDoctorPrescriptions(@PathVariable UUID doctorId) {
        requireActiveDoctor(doctorId);
        return prescriptionService.getDoctorPrescriptions(doctorId);
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

    private AppointmentResponseDto toResponse(com.smp.appointment.AppointmentDao appointment) {
        return new AppointmentResponseDto(
                appointment.getId(),
                appointment.getDoctor().getId(),
                appointment.getDoctor().getName(),
                appointment.getPatient().getId(),
                appointment.getPatient().getName(),
                appointment.getService().getId(),
                appointment.getService().getName(),
                appointment.getAppointmentTime(),
                appointment.getEndTime(),
                appointment.getStatus(),
                appointment.getReason(),
                appointment.getNotes(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt());
    }
}
