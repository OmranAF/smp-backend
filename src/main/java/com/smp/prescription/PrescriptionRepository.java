package com.smp.prescription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionRepository extends JpaRepository<PrescriptionDao, UUID> {

    List<PrescriptionDao> findByDoctor_IdOrderByIssuedAtDesc(UUID doctorId);

    List<PrescriptionDao> findByPatient_IdOrderByIssuedAtDesc(UUID patientId);

    Optional<PrescriptionDao> findByTicketId(UUID ticketId);
}
