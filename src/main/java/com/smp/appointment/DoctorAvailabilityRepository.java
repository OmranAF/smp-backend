package com.smp.appointment;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailabilityDao, UUID> {
	List<DoctorAvailabilityDao> findByDoctor_Id(UUID doctorId);

	java.util.Optional<DoctorAvailabilityDao> findByIdAndDoctor_Id(UUID id, UUID doctorId);

	List<DoctorAvailabilityDao> findByDoctor_IdAndDayOfWeekAndActiveTrue(UUID doctorId, DayOfWeek dayOfWeek);
}