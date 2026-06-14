package com.smp.patient;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientDocumentRepository extends JpaRepository<PatientDocumentDao, UUID> {
}