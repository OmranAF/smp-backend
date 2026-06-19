package com.smp.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<DoctorDao, UUID> {
    Optional<DoctorDao> findByUser_Email(String email);

    Optional<DoctorDao> findByUser_Id(UUID userId);

    List<DoctorDao> findByActiveOrderByNameAsc(boolean active);

    List<DoctorDao> findByActiveTrueAndUser_EnabledTrueOrderByNameAsc();
}