package com.smp.appointment;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.smp.appointment.dto.FreeAppointmentSlotDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DoctorSlotService {

    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public List<FreeAppointmentSlotDto> getFreeSlots(UUID doctorId, LocalDate date) {
        List<DoctorAvailabilityDao> availabilities = doctorAvailabilityRepository
                .findByDoctor_IdAndDayOfWeekAndActiveTrue(doctorId, date.getDayOfWeek())
                .stream()
                .sorted(Comparator.comparing(DoctorAvailabilityDao::getStartTime))
                .toList();

        if (availabilities.isEmpty()) {
            return List.of();
        }

        List<AppointmentDao> appointments = getNonCancelledAppointmentsForDate(doctorId, date);

        return availabilities.stream()
                .flatMap(availability -> createFreeSlots(date, availability, appointments).stream())
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isSlotAvailable(UUID doctorId, LocalDateTime start, LocalDateTime end, UUID ignoredAppointmentId) {
        if (!isInsideAvailabilityWindow(doctorId, start, end)) {
            return false;
        }

        List<AppointmentDao> appointments = getNonCancelledAppointmentsForDate(doctorId, start.toLocalDate());
        for (AppointmentDao appointment : appointments) {
            if (ignoredAppointmentId != null && ignoredAppointmentId.equals(appointment.getId())) {
                continue;
            }
            if (overlaps(start, end, appointment.getAppointmentTime(), appointment.getEndTime())) {
                return false;
            }
        }

        return true;
    }

    private List<FreeAppointmentSlotDto> createFreeSlots(
            LocalDate date,
            DoctorAvailabilityDao availability,
            List<AppointmentDao> appointments) {
        LocalDateTime slotStart = LocalDateTime.of(date, availability.getStartTime());
        LocalDateTime windowEnd = LocalDateTime.of(date, availability.getEndTime());
        int stepMinutes = availability.getSlotDurationMinutes();

        if (stepMinutes <= 0) {
            return List.of();
        }

        List<FreeAppointmentSlotDto> slots = new java.util.ArrayList<>();

        while (!slotStart.plusMinutes(stepMinutes).isAfter(windowEnd)) {
            LocalDateTime slotEnd = slotStart.plusMinutes(stepMinutes);
            LocalDateTime currentSlotStart = slotStart;
            LocalDateTime currentSlotEnd = slotEnd;

            boolean occupied = appointments.stream()
                    .anyMatch(appointment -> overlaps(currentSlotStart, currentSlotEnd, appointment.getAppointmentTime(), appointment.getEndTime()));

            if (!occupied) {
                slots.add(new FreeAppointmentSlotDto(currentSlotStart, currentSlotEnd));
            }

            slotStart = slotStart.plusMinutes(stepMinutes);
        }

        return slots;
    }

    private boolean isInsideAvailabilityWindow(UUID doctorId, LocalDateTime start, LocalDateTime end) {
        if (!start.toLocalDate().equals(end.toLocalDate()) || !start.isBefore(end)) {
            return false;
        }

        List<DoctorAvailabilityDao> availabilities = doctorAvailabilityRepository
                .findByDoctor_IdAndDayOfWeekAndActiveTrue(doctorId, start.getDayOfWeek());

        for (DoctorAvailabilityDao availability : availabilities) {
            LocalDateTime windowStart = LocalDateTime.of(start.toLocalDate(), availability.getStartTime());
            LocalDateTime windowEnd = LocalDateTime.of(start.toLocalDate(), availability.getEndTime());

            if (start.isBefore(windowStart) || end.isAfter(windowEnd)) {
                continue;
            }

            long offsetMinutes = Duration.between(windowStart, start).toMinutes();
            if (offsetMinutes < 0 || offsetMinutes % availability.getSlotDurationMinutes() != 0) {
                continue;
            }

            return true;
        }

        return false;
    }

    private List<AppointmentDao> getNonCancelledAppointmentsForDate(UUID doctorId, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        return appointmentRepository.findOverlappingAppointments(doctorId, dayStart, dayEnd).stream()
                .filter(appointment -> !isCancelled(appointment.getStatus()))
                .toList();
    }

    private boolean isCancelled(AppointmentStatus status) {
        return status == AppointmentStatus.CANCELLED_BY_DOCTOR || status == AppointmentStatus.CANCELLED_BY_PATIENT;
    }

    private boolean overlaps(LocalDateTime startA, LocalDateTime endA, LocalDateTime startB, LocalDateTime endB) {
        return startA.isBefore(endB) && startB.isBefore(endA);
    }
}
