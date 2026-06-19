package com.smp.user;

import java.util.UUID;
import java.util.List;

import com.smp.user.dto.PharmacistOpeningHoursRequestDto;
import com.smp.user.dto.PharmacistOpeningHoursResponseDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/pharmacists/{pharmacistId}/opening-hours")
@RequiredArgsConstructor
public class PharmacistOpeningHoursController {

    private final PharmacistRepository pharmacistRepository;
    private final PharmacistOpeningHoursRepository openingHoursRepository;

    @PostMapping
    public PharmacistOpeningHoursResponseDto createOpeningHours(
            @PathVariable UUID pharmacistId,
            Authentication authentication,
            @Valid @RequestBody PharmacistOpeningHoursRequestDto request) {
        PharmacistDao pharmacist = findAuthorizedPharmacist(pharmacistId, authentication);
        validateTimeRange(request.startTime(), request.endTime());

        PharmacistOpeningHoursDao row = new PharmacistOpeningHoursDao();
        row.setPharmacist(pharmacist);
        row.setDayOfWeek(request.dayOfWeek());
        row.setStartTime(request.startTime());
        row.setEndTime(request.endTime());
        row.setActive(request.active());
        return toDto(openingHoursRepository.save(row));
    }

    @GetMapping
    public List<PharmacistOpeningHoursResponseDto> getOpeningHours(
            @PathVariable UUID pharmacistId,
            Authentication authentication) {
        findAuthorizedPharmacist(pharmacistId, authentication);
        return openingHoursRepository.findByPharmacist_IdOrderByDayOfWeekAscStartTimeAsc(pharmacistId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @PutMapping("/{openingHoursId}")
    public PharmacistOpeningHoursResponseDto updateOpeningHours(
            @PathVariable UUID pharmacistId,
            @PathVariable UUID openingHoursId,
            Authentication authentication,
            @Valid @RequestBody PharmacistOpeningHoursRequestDto request) {
        findAuthorizedPharmacist(pharmacistId, authentication);
        validateTimeRange(request.startTime(), request.endTime());

        PharmacistOpeningHoursDao row = openingHoursRepository.findByIdAndPharmacist_Id(openingHoursId, pharmacistId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Opening hours not found"));

        row.setDayOfWeek(request.dayOfWeek());
        row.setStartTime(request.startTime());
        row.setEndTime(request.endTime());
        row.setActive(request.active());
        return toDto(openingHoursRepository.save(row));
    }

    @DeleteMapping("/{openingHoursId}")
    public void deleteOpeningHours(
            @PathVariable UUID pharmacistId,
            @PathVariable UUID openingHoursId,
            Authentication authentication) {
        findAuthorizedPharmacist(pharmacistId, authentication);
        PharmacistOpeningHoursDao row = openingHoursRepository.findByIdAndPharmacist_Id(openingHoursId, pharmacistId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Opening hours not found"));
        openingHoursRepository.delete(row);
    }

    private PharmacistDao findAuthorizedPharmacist(UUID pharmacistId, Authentication authentication) {
        PharmacistDao pharmacist = findPharmacist(pharmacistId);
        String authenticatedEmail = authentication == null ? null : authentication.getName();
        if (authenticatedEmail == null || !authenticatedEmail.equalsIgnoreCase(pharmacist.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage your own opening hours");
        }
        return pharmacist;
    }

    private PharmacistDao findPharmacist(UUID pharmacistId) {
        return pharmacistRepository.findById(pharmacistId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pharmacist not found"));
    }

    private void validateTimeRange(java.time.LocalTime start, java.time.LocalTime end) {
        if (!start.isBefore(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time must be before end time");
        }
    }

    private PharmacistOpeningHoursResponseDto toDto(PharmacistOpeningHoursDao row) {
        return new PharmacistOpeningHoursResponseDto(
                row.getId(),
                row.getDayOfWeek(),
                row.getStartTime(),
                row.getEndTime(),
                row.isActive());
    }
}
