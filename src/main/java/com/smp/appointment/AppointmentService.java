package com.smp.appointment;

import java.util.List;
import java.util.UUID;

import com.smp.appointment.dto.AppointmentRequestDto;
import com.smp.appointment.dto.AppointmentResponseDto;
import com.smp.user.DoctorDao;
import com.smp.user.PatientDao;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final EntityManager entityManager;
    private final DoctorSlotService doctorSlotService;

    @Transactional
    public AppointmentResponseDto createAppointment(AppointmentRequestDto request) {
        AppointmentDao appointment = new AppointmentDao();
        applyRequest(appointment, request);
        return toResponse(appointmentRepository.save(appointment));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> getAllAppointments() {
        return appointmentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponseDto getAppointmentById(UUID id) {
        return appointmentRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
    }

    @Transactional
    public AppointmentResponseDto updateAppointment(UUID id, AppointmentRequestDto request) {
        AppointmentDao appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        applyRequest(appointment, request);
        return toResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public void deleteAppointment(UUID id) {
        if (!appointmentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found");
        }
        appointmentRepository.deleteById(id);
    }

    @Transactional
    public AppointmentResponseDto cancelByDoctor(UUID doctorId, UUID appointmentId, String cancellationReason) {
        AppointmentDao appointment = appointmentRepository.findByIdAndDoctor_Id(appointmentId, doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED_BY_DOCTOR
                || appointment.getStatus() == AppointmentStatus.CANCELLED_BY_PATIENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment is already cancelled");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED || appointment.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed appointments cannot be cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED_BY_DOCTOR);

        String trimmedReason = cancellationReason == null ? "" : cancellationReason.trim();
        if (!trimmedReason.isEmpty()) {
            String cancellationNote = "Doctor cancellation reason: " + trimmedReason;
            if (appointment.getNotes() == null || appointment.getNotes().isBlank()) {
                appointment.setNotes(cancellationNote);
            } else {
                appointment.setNotes(appointment.getNotes() + System.lineSeparator() + cancellationNote);
            }
        }

        return toResponse(appointmentRepository.save(appointment));
    }

    private void applyRequest(AppointmentDao appointment, AppointmentRequestDto request) {
        DoctorDao doctor = entityManager.find(DoctorDao.class, request.doctorId());
        if (doctor == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found");
        }
        if (doctor.getUser() == null || !doctor.getUser().isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor account is disabled by admin");
        }
        if (!doctor.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor account is not active");
        }

        PatientDao patient = entityManager.find(PatientDao.class, request.patientId());
        if (patient == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        }

        DoctorServiceDao service = entityManager.find(DoctorServiceDao.class, request.serviceId());
        if (service == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found");
        }

        if (service.getDoctor() == null || service.getDoctor().getId() == null || !service.getDoctor().getId().equals(doctor.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service does not belong to the selected doctor");
        }

        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setService(service);
        appointment.setAppointmentTime(request.appointmentTime());
        appointment.setEndTime(request.appointmentTime().plusMinutes(service.getDurationMinutes()));

        boolean slotAvailable = doctorSlotService.isSlotAvailable(
                doctor.getId(),
                appointment.getAppointmentTime(),
                appointment.getEndTime(),
                appointment.getId());

        if (!slotAvailable) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected appointment time is not free for this doctor");
        }

        if (request.status() != null) {
            appointment.setStatus(request.status());
        }
        appointment.setReason(request.reason());
        appointment.setNotes(request.notes());
    }

    private AppointmentResponseDto toResponse(AppointmentDao appointment) {
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