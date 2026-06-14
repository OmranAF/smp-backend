package com.smp.patient;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientHealthStatusRepository extends JpaRepository<PatientHealthStatusDao, UUID> {
}