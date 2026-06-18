package com.smp.user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.smp.appointment.DoctorSlotService;
import com.smp.appointment.DoctorServiceRepository;
import com.smp.appointment.dto.FreeAppointmentSlotDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DoctorCatalogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private DoctorServiceRepository doctorServiceRepository;

    @Mock
    private DoctorSlotService doctorSlotService;

    @BeforeEach
    void setUp() {
        DoctorCatalogController controller = new DoctorCatalogController(doctorRepository, doctorServiceRepository, doctorSlotService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldReturnFreeSlotsForDoctorAndDate() throws Exception {
        UUID doctorId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 22);

        FreeAppointmentSlotDto slot = new FreeAppointmentSlotDto(
                LocalDateTime.of(2026, 6, 22, 9, 0),
                LocalDateTime.of(2026, 6, 22, 9, 30));

        when(doctorSlotService.getFreeSlots(eq(doctorId), eq(date))).thenReturn(List.of(slot));

        mockMvc.perform(get("/api/doctors/{doctorId}/free-slots", doctorId)
                        .queryParam("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startTime").value("2026-06-22T09:00:00"))
                .andExpect(jsonPath("$[0].endTime").value("2026-06-22T09:30:00"));

        verify(doctorSlotService).getFreeSlots(eq(doctorId), any(LocalDate.class));
    }
}
