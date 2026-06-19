package com.smp.appointment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.smp.user.DoctorDao;
import com.smp.user.PatientDao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<AppointmentDao, UUID> {

    @Query("""
	    SELECT a
	    FROM AppointmentDao a
	    WHERE a.doctor.id = :doctorId
	      AND a.appointmentTime < :dayEnd
	      AND a.endTime > :dayStart
	    """)
    List<AppointmentDao> findOverlappingAppointments(
	    @Param("doctorId") UUID doctorId,
	    @Param("dayStart") LocalDateTime dayStart,
	    @Param("dayEnd") LocalDateTime dayEnd);

    List<AppointmentDao> findByDoctor_IdOrderByAppointmentTimeDesc(UUID doctorId);

	    @Query("""
		    SELECT DISTINCT a.patient
		    FROM AppointmentDao a
		    WHERE a.doctor.id = :doctorId
		    ORDER BY a.patient.name ASC
		    """)
	    List<PatientDao> findDistinctPatientsByDoctorId(@Param("doctorId") UUID doctorId);

	    @Query("""
		    SELECT DISTINCT a.doctor
		    FROM AppointmentDao a
		    WHERE a.patient.id = :patientId
		    ORDER BY a.doctor.name ASC
		    """)
	    List<DoctorDao> findDistinctDoctorsByPatientId(@Param("patientId") UUID patientId);

	boolean existsByDoctor_IdAndPatient_Id(UUID doctorId, UUID patientId);

	java.util.Optional<AppointmentDao> findByIdAndDoctor_Id(UUID appointmentId, UUID doctorId);
}