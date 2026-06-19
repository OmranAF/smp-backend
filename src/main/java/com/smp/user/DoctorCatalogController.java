package com.smp.user;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


import com.smp.appointment.DoctorSlotService;
import com.smp.appointment.DoctorServiceRepository;
import com.smp.appointment.dto.FreeAppointmentSlotDto;
import com.smp.appointment.dto.DoctorServiceOptionDto;
import com.smp.user.dto.DoctorOptionDto;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorCatalogController {

    private final DoctorRepository doctorRepository;
    private final DoctorServiceRepository doctorServiceRepository;
    private final DoctorSlotService doctorSlotService;

    @GetMapping
    public List<DoctorOptionDto> getDoctors() {
        return doctorRepository.findByActiveTrueAndUser_EnabledTrueOrderByNameAsc().stream()
                .map(doctor -> new DoctorOptionDto(doctor.getId(), doctor.getName(), doctor.getSpecialization()))
                .toList();
    }

    @GetMapping("/{doctorId}/services")
    public List<DoctorServiceOptionDto> getDoctorServices(@PathVariable UUID doctorId) {
        return doctorServiceRepository.findByDoctor_Id(doctorId).stream()
                .map(service -> new DoctorServiceOptionDto(service.getId(), service.getName(), service.getDurationMinutes(), service.getPrice()))
                .toList();
    }

    @GetMapping("/{doctorId}/free-slots")
    public List<FreeAppointmentSlotDto> getDoctorFreeSlots(
            @PathVariable UUID doctorId,
            @RequestParam LocalDate date) {
        return doctorSlotService.getFreeSlots(doctorId, date);
    }
}