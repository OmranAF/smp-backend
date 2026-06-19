package com.smp.user;

import java.time.LocalDateTime;

import com.smp.user.dto.PharmacistLoginRequestDto;
import com.smp.user.dto.PharmacistLoginResponseDto;
import com.smp.user.dto.PharmacistRegistrationRequestDto;
import com.smp.user.dto.PharmacistRegistrationResponseDto;

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
@RequestMapping("/api/pharmacists")
@RequiredArgsConstructor
public class PharmacistPortalController {

    private final UserRepository userRepository;
    private final PharmacistRepository pharmacistRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public PharmacistRegistrationResponseDto register(@Valid @RequestBody PharmacistRegistrationRequestDto request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        PharmacistDao pharmacist = new PharmacistDao();
        pharmacist.setName(request.name().trim());
        pharmacist.setAddress(request.address().trim());
        pharmacist.setEmail(email);
        pharmacist.setPassword(passwordEncoder.encode(request.password()));
        pharmacist.setRole(Role.PHARMACIST);
        pharmacist.setEnabled(true);
        pharmacist.setCreatedAt(LocalDateTime.now());
        pharmacist = pharmacistRepository.save(pharmacist);

        return new PharmacistRegistrationResponseDto(
                pharmacist.getId(),
                pharmacist.getName(),
                pharmacist.getAddress(),
                pharmacist.getEmail());
    }

    @PostMapping("/login")
    public PharmacistLoginResponseDto login(@Valid @RequestBody PharmacistLoginRequestDto request) {
        PharmacistDao pharmacist = pharmacistRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), pharmacist.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (!pharmacist.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Pharmacist account is disabled by admin");
        }

        return new PharmacistLoginResponseDto(
                pharmacist.getId(),
                pharmacist.getName(),
                pharmacist.getAddress(),
                pharmacist.getEmail());
    }
}
