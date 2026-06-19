package com.smp.prescription;

import java.util.UUID;

import com.smp.prescription.dto.PatientPrescriptionResponseDto;
import com.smp.prescription.dto.PharmacistPrescriptionDispenseRequestDto;
import com.smp.user.PharmacistDao;
import com.smp.user.PharmacistRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/pharmacists/{pharmacistId}/prescriptions")
@RequiredArgsConstructor
public class PharmacistPrescriptionController {

    private final PrescriptionService prescriptionService;
    private final PharmacistRepository pharmacistRepository;

    @PostMapping("/dispense")
    public PatientPrescriptionResponseDto dispensePrescription(
            @PathVariable UUID pharmacistId,
            Authentication authentication,
            @Valid @RequestBody PharmacistPrescriptionDispenseRequestDto request) {
        PharmacistDao pharmacist = pharmacistRepository.findById(pharmacistId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pharmacist not found"));

        String authenticatedEmail = authentication == null ? null : authentication.getName();
        if (authenticatedEmail == null || !authenticatedEmail.equalsIgnoreCase(pharmacist.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only dispense with your own pharmacist account");
        }

        return prescriptionService.dispensePrescriptionByTicket(
                pharmacistId,
                request.ticketId(),
                request.token(),
                request.status(),
                request.note());
    }
}
