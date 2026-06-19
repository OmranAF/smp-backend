package com.smp.user;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.smp.user.dto.AdminUserSummaryDto;
import com.smp.user.dto.AdminUserUpdateRequestDto;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AppAdminController {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final PharmacistRepository pharmacistRepository;
        private final HospitalRepository hospitalRepository;
        private final LaborRepository laborRepository;
        private final HealthCenterRepository healthCenterRepository;

    @GetMapping
    public List<AdminUserSummaryDto> getUsers(@RequestParam(required = false) Role role) {
        return userRepository.findAll().stream()
                .filter(user -> role == null || user.getRole() == role)
                .map(this::toSummary)
                .sorted(Comparator.comparing(AdminUserSummaryDto::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AdminUserSummaryDto::email, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @PatchMapping("/{userId}")
        public AdminUserSummaryDto updateUser(
                        @PathVariable UUID userId,
                        @RequestBody AdminUserUpdateRequestDto request,
                        Authentication authentication) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.enabled() != null) {
                        if (Boolean.FALSE.equals(request.enabled()) && authentication != null
                                        && authentication.getName().equalsIgnoreCase(user.getEmail())) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin cannot disable the current logged-in account");
                        }
            user.setEnabled(request.enabled());
            user = userRepository.save(user);
        }

                if (request.doctorApproved() != null) {
                        if (user.getRole() != Role.DOCTOR) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor approval can only be changed for doctor accounts");
                        }

                        DoctorDao doctor = doctorRepository.findByUser_Id(userId)
                                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor profile not found"));
                        doctor.setActive(request.doctorApproved());
                        doctorRepository.save(doctor);
        }

        return toSummary(userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")));
    }

    private AdminUserSummaryDto toSummary(User user) {
        return switch (user.getRole()) {
            case DOCTOR -> doctorRepository.findByUser_Id(user.getId())
                    .map(doctor -> new AdminUserSummaryDto(
                            user.getId(),
                            doctor.getId(),
                            doctor.getName(),
                            user.getEmail(),
                            user.getRole(),
                            user.isEnabled(),
                            doctor.isActive(),
                            doctor.getSpecialization(),
                            doctor.getAddress(),
                            user.getCreatedAt()))
                    .orElseGet(() -> baseSummary(user));
            case PATIENT -> patientRepository.findByUser_Id(user.getId())
                    .map(patient -> new AdminUserSummaryDto(
                            user.getId(),
                            patient.getId(),
                            patient.getName(),
                            user.getEmail(),
                            user.getRole(),
                            user.isEnabled(),
                            null,
                            null,
                            null,
                            user.getCreatedAt()))
                    .orElseGet(() -> baseSummary(user));
            case PHARMACIST -> pharmacistRepository.findById(user.getId())
                    .map(pharmacist -> new AdminUserSummaryDto(
                            user.getId(),
                            pharmacist.getId(),
                            pharmacist.getName(),
                            pharmacist.getEmail(),
                            user.getRole(),
                            user.isEnabled(),
                            null,
                            null,
                            pharmacist.getAddress(),
                            user.getCreatedAt()))
                    .orElseGet(() -> baseSummary(user));
            case HOSPITAL -> hospitalRepository.findById(user.getId())
                    .map(hospital -> new AdminUserSummaryDto(
                            user.getId(),
                            hospital.getId(),
                            hospital.getName(),
                            hospital.getEmail(),
                            user.getRole(),
                            user.isEnabled(),
                            null,
                            null,
                            hospital.getAddress(),
                            user.getCreatedAt()))
                    .orElseGet(() -> baseSummary(user));
            case LABOR -> laborRepository.findById(user.getId())
                    .map(labor -> new AdminUserSummaryDto(
                            user.getId(),
                            labor.getId(),
                            labor.getName(),
                            labor.getEmail(),
                            user.getRole(),
                            user.isEnabled(),
                            null,
                            null,
                            labor.getAddress(),
                            user.getCreatedAt()))
                    .orElseGet(() -> baseSummary(user));
            case HealthCenter -> healthCenterRepository.findById(user.getId())
                    .map(healthCenter -> new AdminUserSummaryDto(
                            user.getId(),
                            healthCenter.getId(),
                            healthCenter.getName(),
                            healthCenter.getEmail(),
                            user.getRole(),
                            user.isEnabled(),
                            null,
                            null,
                            healthCenter.getAddress(),
                            user.getCreatedAt()))
                    .orElseGet(() -> baseSummary(user));
            default -> baseSummary(user);
        };
    }

    private AdminUserSummaryDto baseSummary(User user) {
        return new AdminUserSummaryDto(
                user.getId(),
                user.getId(),
                user.getRole().name(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                null,
                null,
                null,
                user.getCreatedAt());
    }
}
