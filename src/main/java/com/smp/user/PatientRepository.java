package com.smp.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<PatientDao, UUID> {
    Optional<PatientDao> findByUser_EmailAndUser_Password(String email, String password);

    Optional<PatientDao> findByUser_Email(String email);
}