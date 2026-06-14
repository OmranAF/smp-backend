package com.smp.patient;

import java.util.List;

import com.smp.patient.dto.AllergyOptionDto;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/allergies")
@RequiredArgsConstructor
public class AllergyController {

    private final AllergyRepository allergyRepository;

    @GetMapping
    public List<AllergyOptionDto> getAllergies() {
        return allergyRepository.findAll().stream()
                .map(allergy -> new AllergyOptionDto(allergy.getId(), allergy.getName(), allergy.getDescription()))
                .toList();
    }
}