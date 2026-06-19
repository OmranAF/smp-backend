package com.smp.user;

import java.time.LocalDateTime;

import com.smp.user.dto.DoctorRegistrationRequestDto;
import com.smp.user.dto.DoctorRegistrationResponseDto;

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
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorRegistrationController {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public DoctorRegistrationResponseDto register(@Valid @RequestBody DoctorRegistrationRequestDto request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.DOCTOR);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        DoctorDao doctor = new DoctorDao();
        doctor.setName(request.name().trim());
        doctor.setAddress(request.address().trim());
        doctor.setSpecialization(request.specialization().trim());
        doctor.setActive(false);
        doctor.setUser(user);
        doctor = doctorRepository.save(doctor);

        return new DoctorRegistrationResponseDto(
                doctor.getId(),
                doctor.getName(),
                user.getEmail(),
                doctor.getSpecialization(),
                doctor.isActive(),
                "Registration submitted. Waiting for doctor admin activation.");
    }
}
