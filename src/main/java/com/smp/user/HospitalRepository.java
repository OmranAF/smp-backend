package com.smp.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalRepository extends JpaRepository<HospitalDao, UUID> {
    Optional<HospitalDao> findByEmail(String email);
}
