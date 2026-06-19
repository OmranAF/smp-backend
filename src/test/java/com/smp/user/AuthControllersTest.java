package com.smp.user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.smp.appointment.AppointmentRepository;
import com.smp.appointment.AppointmentService;
import com.smp.prescription.PrescriptionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllersTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PharmacistRepository pharmacistRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private LaborRepository laborRepository;

    @Mock
    private HealthCenterRepository healthCenterRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private PrescriptionService prescriptionService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private MockMvc doctorRegistrationMvc;
    private MockMvc doctorPortalMvc;
    private MockMvc patientPortalMvc;
    private MockMvc pharmacistPortalMvc;
    private MockMvc organizationPortalMvc;

    @BeforeEach
    void setUp() {
        doctorRegistrationMvc = MockMvcBuilders.standaloneSetup(
                new DoctorRegistrationController(userRepository, doctorRepository, passwordEncoder))
                .build();

        doctorPortalMvc = MockMvcBuilders.standaloneSetup(
                new DoctorPortalController(
                        doctorRepository,
                        appointmentRepository,
                        appointmentService,
                        prescriptionService,
                        passwordEncoder))
                .build();

        patientPortalMvc = MockMvcBuilders.standaloneSetup(
                new PatientPortalController(patientRepository, passwordEncoder))
                .build();

        pharmacistPortalMvc = MockMvcBuilders.standaloneSetup(
                new PharmacistPortalController(userRepository, pharmacistRepository, passwordEncoder))
                .build();

        organizationPortalMvc = MockMvcBuilders.standaloneSetup(
          new OrganizationPortalController(
            userRepository,
            hospitalRepository,
            laborRepository,
            healthCenterRepository,
            passwordEncoder))
          .build();
    }

    @Test
    void shouldHashDoctorPasswordOnRegistration() throws Exception {
        when(userRepository.existsByEmail("newdoctor@example.com")).thenReturn(false);
        when(passwordEncoder.encode("doctor123")).thenReturn("encoded-doctor123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(LocalDateTime.of(2026, 6, 19, 14, 0));
            return saved;
        });
        when(doctorRepository.save(any(DoctorDao.class))).thenAnswer(invocation -> {
            DoctorDao saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        doctorRegistrationMvc.perform(post("/api/doctors/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Dr. New",
                                  "address": "Main Street 5",
                                  "specialization": "Neurology",
                                  "email": "newdoctor@example.com",
                                  "password": "doctor123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("newdoctor@example.com"))
                .andExpect(jsonPath("$.active").value(false));

        verify(passwordEncoder).encode("doctor123");
        verify(userRepository).save(argThat(user -> "encoded-doctor123".equals(user.getPassword())));
    }

    @Test
    void shouldLoginDoctorUsingPasswordEncoderMatches() throws Exception {
        User user = new User();
        user.setEmail("sara@example.com");
        user.setPassword("encoded-doctor123");

        DoctorDao doctor = new DoctorDao();
        doctor.setId(UUID.randomUUID());
        doctor.setName("Dr. Sara Ali");
        doctor.setSpecialization("Dermatology");
        doctor.setActive(true);
        doctor.setUser(user);

        when(doctorRepository.findByUser_Email("sara@example.com")).thenReturn(Optional.of(doctor));
        when(passwordEncoder.matches("doctor123", "encoded-doctor123")).thenReturn(true);

        doctorPortalMvc.perform(post("/api/doctors/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "sara@example.com",
                                  "password": "doctor123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("sara@example.com"))
                .andExpect(jsonPath("$.specialization").value("Dermatology"));

        verify(passwordEncoder).matches("doctor123", "encoded-doctor123");
    }

    @Test
    void shouldLoginPatientUsingPasswordEncoderMatches() throws Exception {
        User user = new User();
        user.setEmail("patient@example.com");
        user.setPassword("encoded-patient123");

        PatientDao patient = new PatientDao();
        patient.setId(UUID.randomUUID());
        patient.setName("Demo Patient");
        patient.setDateOfBirth(LocalDate.of(1995, 6, 14));
        patient.setUser(user);

        when(patientRepository.findByUser_Email("patient@example.com")).thenReturn(Optional.of(patient));
        when(passwordEncoder.matches("patient123", "encoded-patient123")).thenReturn(true);

        patientPortalMvc.perform(post("/api/patients/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "patient@example.com",
                                  "password": "patient123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("patient@example.com"))
                .andExpect(jsonPath("$.name").value("Demo Patient"));

        verify(passwordEncoder).matches("patient123", "encoded-patient123");
    }

    @Test
    void shouldHashPharmacistPasswordOnRegistration() throws Exception {
        when(userRepository.existsByEmail("pharmacist@example.com")).thenReturn(false);
        when(passwordEncoder.encode("pharma123")).thenReturn("encoded-pharma123");
        when(pharmacistRepository.save(any(PharmacistDao.class))).thenAnswer(invocation -> {
            PharmacistDao saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(LocalDateTime.of(2026, 6, 19, 14, 10));
            return saved;
        });

        pharmacistPortalMvc.perform(post("/api/pharmacists/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Pharma One",
                                  "address": "Market Street 7",
                                  "email": "pharmacist@example.com",
                                  "password": "pharma123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("pharmacist@example.com"))
                .andExpect(jsonPath("$.name").value("Pharma One"));

        verify(passwordEncoder).encode("pharma123");
        verify(pharmacistRepository).save(argThat(pharmacist -> "encoded-pharma123".equals(pharmacist.getPassword())));
    }

    @Test
    void shouldLoginPharmacistUsingPasswordEncoderMatches() throws Exception {
        PharmacistDao pharmacist = new PharmacistDao();
        pharmacist.setId(UUID.randomUUID());
        pharmacist.setName("Pharma One");
        pharmacist.setAddress("Market Street 7");
        pharmacist.setEmail("pharmacist@example.com");
        pharmacist.setPassword("encoded-pharma123");

        when(pharmacistRepository.findByEmail("pharmacist@example.com")).thenReturn(Optional.of(pharmacist));
        when(passwordEncoder.matches("pharma123", "encoded-pharma123")).thenReturn(true);

        pharmacistPortalMvc.perform(post("/api/pharmacists/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "pharmacist@example.com",
                                  "password": "pharma123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("pharmacist@example.com"))
                .andExpect(jsonPath("$.name").value("Pharma One"));

        verify(pharmacistRepository).findByEmail(eq("pharmacist@example.com"));
        verify(passwordEncoder).matches("pharma123", "encoded-pharma123");
    }

      @Test
      void shouldHashHospitalPasswordOnRegistration() throws Exception {
        when(userRepository.existsByEmail("hospital@example.com")).thenReturn(false);
        when(passwordEncoder.encode("hospital123")).thenReturn("encoded-hospital123");
        when(hospitalRepository.save(any(HospitalDao.class))).thenAnswer(invocation -> {
          HospitalDao saved = invocation.getArgument(0);
          saved.setId(UUID.randomUUID());
          saved.setCreatedAt(LocalDateTime.of(2026, 6, 19, 14, 20));
          return saved;
        });

        organizationPortalMvc.perform(post("/api/organizations/HOSPITAL/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Central Hospital",
                      "address": "Care Avenue 10",
                      "email": "hospital@example.com",
                      "password": "hospital123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("hospital@example.com"))
            .andExpect(jsonPath("$.role").value("HOSPITAL"));

        verify(passwordEncoder).encode("hospital123");
        verify(hospitalRepository).save(argThat(hospital -> "encoded-hospital123".equals(hospital.getPassword())));
      }

      @Test
      void shouldLoginHospitalUsingPasswordEncoderMatches() throws Exception {
        HospitalDao hospital = new HospitalDao();
        hospital.setId(UUID.randomUUID());
        hospital.setName("Central Hospital");
        hospital.setAddress("Care Avenue 10");
        hospital.setEmail("hospital@example.com");
        hospital.setPassword("encoded-hospital123");
        hospital.setEnabled(true);

        when(hospitalRepository.findByEmail("hospital@example.com")).thenReturn(Optional.of(hospital));
        when(passwordEncoder.matches("hospital123", "encoded-hospital123")).thenReturn(true);

        organizationPortalMvc.perform(post("/api/organizations/HOSPITAL/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "hospital@example.com",
                      "password": "hospital123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("hospital@example.com"))
            .andExpect(jsonPath("$.role").value("HOSPITAL"));

        verify(hospitalRepository).findByEmail(eq("hospital@example.com"));
        verify(passwordEncoder).matches("hospital123", "encoded-hospital123");
      }
}
