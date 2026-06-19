package com.smp.appointment;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.smp.appointment.dto.DoctorAvailabilityRequestDto;
import com.smp.appointment.dto.DoctorAvailabilityResponseDto;
import com.smp.user.DoctorDao;
import com.smp.user.DoctorRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/doctors/{doctorId}")
@RequiredArgsConstructor
public class DoctorAvailabilityController {

    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;

    @PostMapping("/availabilities")
    public DoctorAvailabilityResponseDto createAvailability(
            @PathVariable UUID doctorId,
            @Valid @RequestBody DoctorAvailabilityRequestDto request) {
        DoctorDao doctor = requireActiveDoctor(doctorId);

        if (!request.startTime().isBefore(request.endTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time must be before end time");
        }

        DoctorAvailabilityDao availability = new DoctorAvailabilityDao();
        availability.setDoctor(doctor);
        availability.setDayOfWeek(request.dayOfWeek());
        availability.setStartTime(request.startTime());
        availability.setEndTime(request.endTime());
        availability.setSlotDurationMinutes(request.slotDurationMinutes());
        availability.setActive(request.active() == null || request.active());

        return toResponse(doctorAvailabilityRepository.save(availability));
    }

    @GetMapping("/availabilities")
    public List<DoctorAvailabilityResponseDto> getDoctorAvailabilities(@PathVariable UUID doctorId) {
        requireActiveDoctor(doctorId);

        return doctorAvailabilityRepository.findByDoctor_Id(doctorId).stream()
                .sorted(Comparator
                        .comparing(DoctorAvailabilityDao::getDayOfWeek)
                        .thenComparing(DoctorAvailabilityDao::getStartTime))
                .map(this::toResponse)
                .toList();
    }

    @PutMapping("/availabilities/{availabilityId}")
    public DoctorAvailabilityResponseDto updateAvailability(
            @PathVariable UUID doctorId,
            @PathVariable UUID availabilityId,
            @Valid @RequestBody DoctorAvailabilityRequestDto request) {
        requireActiveDoctor(doctorId);

        if (!request.startTime().isBefore(request.endTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time must be before end time");
        }

        DoctorAvailabilityDao availability = doctorAvailabilityRepository.findByIdAndDoctor_Id(availabilityId, doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Availability not found"));

        availability.setDayOfWeek(request.dayOfWeek());
        availability.setStartTime(request.startTime());
        availability.setEndTime(request.endTime());
        availability.setSlotDurationMinutes(request.slotDurationMinutes());
        availability.setActive(request.active() == null || request.active());

        return toResponse(doctorAvailabilityRepository.save(availability));
    }

    @DeleteMapping("/availabilities/{availabilityId}")
    public void deleteAvailability(
            @PathVariable UUID doctorId,
            @PathVariable UUID availabilityId) {
        requireActiveDoctor(doctorId);

        DoctorAvailabilityDao availability = doctorAvailabilityRepository.findByIdAndDoctor_Id(availabilityId, doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Availability not found"));
        doctorAvailabilityRepository.delete(availability);
    }

    private DoctorDao requireActiveDoctor(UUID doctorId) {
        DoctorDao doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));
        if (doctor.getUser() == null || !doctor.getUser().isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor account is disabled by admin");
        }
        if (!doctor.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor account is waiting for admin activation");
        }
        return doctor;
    }

    private DoctorAvailabilityResponseDto toResponse(DoctorAvailabilityDao availability) {
        return new DoctorAvailabilityResponseDto(
                availability.getId(),
                availability.getDayOfWeek(),
                availability.getStartTime(),
                availability.getEndTime(),
                availability.getSlotDurationMinutes(),
                availability.isActive());
    }
}
