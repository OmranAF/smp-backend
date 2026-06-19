package com.smp.user;

import java.time.LocalDateTime;

import com.smp.user.dto.OrganizationLoginRequestDto;
import com.smp.user.dto.OrganizationLoginResponseDto;
import com.smp.user.dto.OrganizationRegistrationRequestDto;
import com.smp.user.dto.OrganizationRegistrationResponseDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/organizations/{role}")
@RequiredArgsConstructor
public class OrganizationPortalController {

    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final LaborRepository laborRepository;
    private final HealthCenterRepository healthCenterRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public OrganizationRegistrationResponseDto register(
            @PathVariable String role,
            @Valid @RequestBody OrganizationRegistrationRequestDto request) {
        Role normalizedRole = parseSupportedRole(role);
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        return switch (normalizedRole) {
            case HOSPITAL -> {
                HospitalDao hospital = new HospitalDao();
                hospital.setName(request.name().trim());
                hospital.setAddress(request.address().trim());
                hospital.setEmail(email);
                hospital.setPassword(passwordEncoder.encode(request.password()));
                hospital.setRole(Role.HOSPITAL);
                hospital.setEnabled(true);
                hospital.setCreatedAt(LocalDateTime.now());
                hospital = hospitalRepository.save(hospital);
                yield new OrganizationRegistrationResponseDto(hospital.getId(), hospital.getName(), hospital.getAddress(), hospital.getEmail(), Role.HOSPITAL);
            }
            case LABOR -> {
                LaborDao labor = new LaborDao();
                labor.setName(request.name().trim());
                labor.setAddress(request.address().trim());
                labor.setEmail(email);
                labor.setPassword(passwordEncoder.encode(request.password()));
                labor.setRole(Role.LABOR);
                labor.setEnabled(true);
                labor.setCreatedAt(LocalDateTime.now());
                labor = laborRepository.save(labor);
                yield new OrganizationRegistrationResponseDto(labor.getId(), labor.getName(), labor.getAddress(), labor.getEmail(), Role.LABOR);
            }
            case HealthCenter -> {
                HealthCenterDao healthCenter = new HealthCenterDao();
                healthCenter.setName(request.name().trim());
                healthCenter.setAddress(request.address().trim());
                healthCenter.setEmail(email);
                healthCenter.setPassword(passwordEncoder.encode(request.password()));
                healthCenter.setRole(Role.HealthCenter);
                healthCenter.setEnabled(true);
                healthCenter.setCreatedAt(LocalDateTime.now());
                healthCenter = healthCenterRepository.save(healthCenter);
                yield new OrganizationRegistrationResponseDto(healthCenter.getId(), healthCenter.getName(), healthCenter.getAddress(), healthCenter.getEmail(), Role.HealthCenter);
            }
            default -> throw unsupportedRole();
        };
    }

    @PostMapping("/login")
    public OrganizationLoginResponseDto login(
            @PathVariable String role,
            @Valid @RequestBody OrganizationLoginRequestDto request) {
        Role normalizedRole = parseSupportedRole(role);
        String email = request.email().trim().toLowerCase();

        return switch (normalizedRole) {
            case HOSPITAL -> hospitalRepository.findByEmail(email)
                    .map(hospital -> toLoginResponse(hospital, request.password(), Role.HOSPITAL))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
            case LABOR -> laborRepository.findByEmail(email)
                    .map(labor -> toLoginResponse(labor, request.password(), Role.LABOR))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
            case HealthCenter -> healthCenterRepository.findByEmail(email)
                    .map(healthCenter -> toLoginResponse(healthCenter, request.password(), Role.HealthCenter))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
            default -> throw unsupportedRole();
        };
    }

    private OrganizationLoginResponseDto toLoginResponse(HospitalDao hospital, String rawPassword, Role role) {
        validateAccount(hospital.isEnabled(), hospital.getPassword(), rawPassword);
        return new OrganizationLoginResponseDto(hospital.getId(), hospital.getName(), hospital.getAddress(), hospital.getEmail(), role);
    }

    private OrganizationLoginResponseDto toLoginResponse(LaborDao labor, String rawPassword, Role role) {
        validateAccount(labor.isEnabled(), labor.getPassword(), rawPassword);
        return new OrganizationLoginResponseDto(labor.getId(), labor.getName(), labor.getAddress(), labor.getEmail(), role);
    }

    private OrganizationLoginResponseDto toLoginResponse(HealthCenterDao healthCenter, String rawPassword, Role role) {
        validateAccount(healthCenter.isEnabled(), healthCenter.getPassword(), rawPassword);
        return new OrganizationLoginResponseDto(healthCenter.getId(), healthCenter.getName(), healthCenter.getAddress(), healthCenter.getEmail(), role);
    }

    private void validateAccount(boolean enabled, String encodedPassword, String rawPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled by admin");
        }
    }

    private Role parseSupportedRole(String value) {
        if (value == null || value.isBlank()) {
            throw unsupportedRole();
        }

        return switch (value.trim().toUpperCase()) {
            case "HOSPITAL" -> Role.HOSPITAL;
            case "LABOR" -> Role.LABOR;
            case "HEALTHCENTER", "HEALTH_CENTER" -> Role.HealthCenter;
            default -> throw unsupportedRole();
        };
    }

    private ResponseStatusException unsupportedRole() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supported organization roles are HOSPITAL, LABOR, and HealthCenter");
    }
}
