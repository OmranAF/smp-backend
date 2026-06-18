package com.smp.appointment;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorServiceRepository extends JpaRepository<DoctorServiceDao, UUID> {
	List<DoctorServiceDao> findByDoctor_Id(UUID doctorId);
}