package com.smp.prescription;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.smp.appointment.AppointmentDao;
import com.smp.user.DoctorDao;
import com.smp.user.PatientDao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
public class PrescriptionDao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private DoctorDao doctor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientDao patient;

    @ManyToOne
    @JoinColumn(name = "appointment_id")
    private AppointmentDao appointment;

    @Column(nullable = false, length = 255)
    private String medicationName;

    @Column(nullable = false, length = 255)
    private String dosage;

    @Column(nullable = false, length = 1500)
    private String instructions;

    @Column(nullable = false, unique = true)
    private UUID ticketId;

    @Column(nullable = false, length = 200)
    private String accessToken;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    private LocalDate validUntil;

    @PrePersist
    private void onCreate() {
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
        if (ticketId == null) {
            ticketId = UUID.randomUUID();
        }
        if (accessToken == null || accessToken.isBlank()) {
            accessToken = UUID.randomUUID().toString();
        }
    }
}
