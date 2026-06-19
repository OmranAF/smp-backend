package com.smp.user;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.smp.user.dto.DoctorActivationUpdateRequestDto;
import com.smp.user.dto.DoctorAdminRegistrationDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/doctors")
@RequiredArgsConstructor
public class DoctorAdminController {

    private final DoctorRepository doctorRepository;

    @GetMapping("/registrations")
    public List<DoctorAdminRegistrationDto> getRegistrations(@RequestParam(required = false) Boolean active) {
        List<DoctorDao> doctors = active == null
                ? doctorRepository.findAll().stream()
                        .sorted(Comparator.comparing(DoctorDao::getName, String.CASE_INSENSITIVE_ORDER))
                        .toList()
                : doctorRepository.findByActiveOrderByNameAsc(active);

        return doctors.stream()
                .map(this::toDto)
                .toList();
    }

    @PatchMapping("/{doctorId}/activation")
    public DoctorAdminRegistrationDto updateActivation(
            @PathVariable UUID doctorId,
            @Valid @RequestBody DoctorActivationUpdateRequestDto request) {
        DoctorDao doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));

        doctor.setActive(request.active());
        return toDto(doctorRepository.save(doctor));
    }

    private DoctorAdminRegistrationDto toDto(DoctorDao doctor) {
        return new DoctorAdminRegistrationDto(
                doctor.getId(),
                doctor.getName(),
                doctor.getUser().getEmail(),
                doctor.getSpecialization(),
                doctor.isActive(),
                doctor.getUser().getCreatedAt());
    }
}
