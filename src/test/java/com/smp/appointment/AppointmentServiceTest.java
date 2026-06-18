package com.smp.appointment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.smp.appointment.dto.AppointmentRequestDto;
import com.smp.user.DoctorDao;
import com.smp.user.PatientDao;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private DoctorSlotService doctorSlotService;

    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentService(appointmentRepository, entityManager, doctorSlotService);
    }

    @Test
    void shouldRejectBookingWhenRequestedSlotIsNotAvailable() {
        UUID doctorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        DoctorDao doctor = new DoctorDao();
        doctor.setId(doctorId);

        PatientDao patient = new PatientDao();
        patient.setId(patientId);

        DoctorServiceDao service = new DoctorServiceDao();
        service.setId(serviceId);
        service.setDoctor(doctor);
        service.setName("General Consultation");
        service.setDurationMinutes(30);
        service.setPrice(new BigDecimal("49.00"));

        LocalDateTime start = LocalDateTime.of(2026, 6, 22, 10, 0);

        AppointmentRequestDto request = new AppointmentRequestDto(
                doctorId,
                patientId,
                serviceId,
                start,
                AppointmentStatus.PENDING,
                "Routine check",
                "No notes");

        when(entityManager.find(DoctorDao.class, doctorId)).thenReturn(doctor);
        when(entityManager.find(PatientDao.class, patientId)).thenReturn(patient);
        when(entityManager.find(DoctorServiceDao.class, serviceId)).thenReturn(service);
        when(doctorSlotService.isSlotAvailable(any(), any(), any(), any())).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> appointmentService.createAppointment(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Selected appointment time is not free for this doctor", ex.getReason());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void shouldCancelPendingAppointmentByDoctor() {
        UUID doctorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();

        DoctorDao doctor = new DoctorDao();
        doctor.setId(doctorId);
        doctor.setName("Dr. Sara Ali");

        PatientDao patient = new PatientDao();
        patient.setId(patientId);
        patient.setName("Demo Patient");

        DoctorServiceDao service = new DoctorServiceDao();
        service.setId(serviceId);
        service.setName("General Consultation");
        service.setDoctor(doctor);

        AppointmentDao appointment = new AppointmentDao();
        appointment.setId(appointmentId);
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setService(service);
        appointment.setAppointmentTime(LocalDateTime.of(2026, 6, 22, 10, 0));
        appointment.setEndTime(LocalDateTime.of(2026, 6, 22, 10, 30));
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setReason("Checkup");
        appointment.setNotes("Existing note");
        appointment.setCreatedAt(LocalDateTime.of(2026, 6, 20, 9, 0));
        appointment.setUpdatedAt(LocalDateTime.of(2026, 6, 20, 9, 0));

        when(appointmentRepository.findByIdAndDoctor_Id(appointmentId, doctorId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(appointment)).thenReturn(appointment);

        var response = appointmentService.cancelByDoctor(doctorId, appointmentId, "Doctor has emergency");

        assertEquals(AppointmentStatus.CANCELLED_BY_DOCTOR, response.status());
        assertTrue(response.notes().contains("Doctor cancellation reason: Doctor has emergency"));
        verify(appointmentRepository).save(appointment);
    }
}
