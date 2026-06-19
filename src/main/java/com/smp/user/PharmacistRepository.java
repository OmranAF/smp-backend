package com.smp.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmacistRepository extends JpaRepository<PharmacistDao, UUID> {

    Optional<PharmacistDao> findByEmail(String email);
}
