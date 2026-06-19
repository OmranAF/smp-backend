package com.smp.user;


import com.smp.user.dto.PatientLoginRequestDto;
import com.smp.user.dto.PatientLoginResponseDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientPortalController {

    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public PatientLoginResponseDto login(@Valid @RequestBody PatientLoginRequestDto request) {
        PatientDao patient = patientRepository.findByUser_Email(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), patient.getUser().getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (!patient.getUser().isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Patient account is disabled by admin");
        }

        return new PatientLoginResponseDto(patient.getId(), patient.getName(), patient.getUser().getEmail());
    }
}