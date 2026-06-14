package com.smp.appointment;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorServiceRepository extends JpaRepository<DoctorServiceDao, UUID> {
}