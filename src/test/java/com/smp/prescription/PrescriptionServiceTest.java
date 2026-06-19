package com.smp.prescription;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.smp.appointment.AppointmentDao;
import com.smp.appointment.AppointmentRepository;
import com.smp.prescription.dto.DoctorPrescriptionRequestDto;
import com.smp.user.DoctorDao;
import com.smp.user.DoctorRepository;
import com.smp.user.PharmacistRepository;
import com.smp.user.PatientDao;
import com.smp.user.PatientRepository;
import com.smp.user.Role;
import com.smp.user.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PharmacistRepository pharmacistRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    private PrescriptionService prescriptionService;

    @BeforeEach
    void setUp() {
        prescriptionService = new PrescriptionService(
                prescriptionRepository,
                doctorRepository,
            pharmacistRepository,
                patientRepository,
                appointmentRepository);
    }

    @Test
    void shouldCreatePrescriptionForDoctorPatientAppointment() {
        UUID doctorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();

        DoctorDao doctor = new DoctorDao();
        doctor.setId(doctorId);
        doctor.setName("Dr. Sara Ali");
        doctor.setActive(true);
        doctor.setUser(User.builder()
            .email("doctor@test.local")
            .password("hashed")
            .role(Role.DOCTOR)
            .enabled(true)
            .build());

        PatientDao patient = new PatientDao();
        patient.setId(patientId);
        patient.setName("Demo Patient");

        AppointmentDao appointment = new AppointmentDao();
        appointment.setId(appointmentId);
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);

        DoctorPrescriptionRequestDto request = new DoctorPrescriptionRequestDto(
                patientId,
                appointmentId,
                "Amoxicillin",
                "500mg twice daily",
                "Take after meals for 5 days",
                LocalDate.of(2026, 7, 1));

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(prescriptionRepository.save(org.mockito.ArgumentMatchers.any(PrescriptionDao.class)))
                .thenAnswer(invocation -> {
                    PrescriptionDao saved = invocation.getArgument(0);
                    saved.setId(UUID.randomUUID());
                    saved.setIssuedAt(LocalDateTime.of(2026, 6, 18, 13, 0));
                    return saved;
                });

        var response = prescriptionService.createPrescription(doctorId, request);

        assertEquals(doctorId, response.doctorId());
        assertEquals(patientId, response.patientId());
        assertEquals(appointmentId, response.appointmentId());
        org.junit.jupiter.api.Assertions.assertNotNull(response.ticketId());
        org.junit.jupiter.api.Assertions.assertTrue(response.qrCodeImageUrl().contains("/api/prescriptions/tickets/"));
        assertEquals("Amoxicillin", response.medicationName());
        assertEquals("500mg twice daily", response.dosage());
        assertEquals("Take after meals for 5 days", response.instructions());
        assertEquals(LocalDate.of(2026, 7, 1), response.validUntil());
        verify(prescriptionRepository).save(org.mockito.ArgumentMatchers.any(PrescriptionDao.class));
    }

    @Test
    void shouldIncludeDoctorNameInQrPayload() {
        UUID ticketId = UUID.randomUUID();

        DoctorDao doctor = new DoctorDao();
        doctor.setName("Dr. Sara Ali");

        PatientDao patient = new PatientDao();
        patient.setName("Demo Patient");

        PrescriptionDao prescription = new PrescriptionDao();
        prescription.setDoctor(doctor);
        prescription.setPatient(patient);
        prescription.setMedicationName("Amoxicillin");
        prescription.setTicketId(ticketId);
        prescription.setAccessToken("token-123");
        prescription.setFulfillmentStatus(PrescriptionFulfillmentStatus.PENDING);

        String payload = prescriptionService.buildQrPayloadUrl(prescription);

        assertTrue(payload.contains("Doctor: Dr. Sara Ali"));
        assertTrue(payload.contains("Patient: Demo Patient"));
        assertTrue(payload.contains("Medication: Amoxicillin"));
        assertTrue(payload.contains("Ticket ID: " + ticketId));
        assertTrue(payload.contains("Token: token-123"));
    }
}
