package com.smp.user;


import com.smp.user.dto.PatientLoginRequestDto;
import com.smp.user.dto.PatientLoginResponseDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/login")
    public PatientLoginResponseDto login(@Valid @RequestBody PatientLoginRequestDto request) {
        PatientDao patient = patientRepository.findByUser_EmailAndUser_Password(request.email(), request.password())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return new PatientLoginResponseDto(patient.getId(), patient.getName(), patient.getUser().getEmail());
    }
}