package com.smp.user;

import java.util.List;
import java.util.UUID;


import com.smp.appointment.DoctorServiceRepository;
import com.smp.appointment.dto.DoctorServiceOptionDto;
import com.smp.user.dto.DoctorOptionDto;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorCatalogController {

    private final DoctorRepository doctorRepository;
    private final DoctorServiceRepository doctorServiceRepository;

    @GetMapping
    public List<DoctorOptionDto> getDoctors() {
        return doctorRepository.findAll().stream()
                .map(doctor -> new DoctorOptionDto(doctor.getId(), doctor.getName(), doctor.getSpecialization()))
                .toList();
    }

    @GetMapping("/{doctorId}/services")
    public List<DoctorServiceOptionDto> getDoctorServices(@PathVariable UUID doctorId) {
        return doctorServiceRepository.findAll().stream()
                .filter(service -> service.getDoctor() != null && doctorId.equals(service.getDoctor().getId()))
                .map(service -> new DoctorServiceOptionDto(service.getId(), service.getName(), service.getDurationMinutes(), service.getPrice()))
                .toList();
    }
}