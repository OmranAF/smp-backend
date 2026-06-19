package com.smp.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmacistOpeningHoursRepository extends JpaRepository<PharmacistOpeningHoursDao, UUID> {

    List<PharmacistOpeningHoursDao> findByPharmacist_IdOrderByDayOfWeekAscStartTimeAsc(UUID pharmacistId);

    Optional<PharmacistOpeningHoursDao> findByIdAndPharmacist_Id(UUID id, UUID pharmacistId);
}
