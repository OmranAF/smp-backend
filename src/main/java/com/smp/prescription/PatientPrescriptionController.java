package com.smp.prescription;

import java.util.List;
import java.util.UUID;

import com.smp.prescription.dto.PatientPrescriptionResponseDto;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients/{patientId}/prescriptions")
@RequiredArgsConstructor
public class PatientPrescriptionController {

    private final PrescriptionService prescriptionService;

    @GetMapping
    public List<PatientPrescriptionResponseDto> getPatientPrescriptions(@PathVariable UUID patientId) {
        return prescriptionService.getPatientPrescriptions(patientId);
    }
}
