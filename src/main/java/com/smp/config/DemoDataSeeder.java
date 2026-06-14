package com.smp.config;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.smp.appointment.DoctorServiceDao;
import com.smp.appointment.DoctorServiceRepository;
import com.smp.patient.AllergyDao;
import com.smp.patient.AllergyRepository;
import com.smp.user.DoctorDao;
import com.smp.user.DoctorRepository;
import com.smp.user.PatientDao;
import com.smp.user.PatientRepository;
import com.smp.user.Role;
import com.smp.user.User;
import com.smp.user.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DemoDataSeeder {

    @Bean
    CommandLineRunner seedData(
            UserRepository userRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            DoctorServiceRepository doctorServiceRepository,
            AllergyRepository allergyRepository) {
        return args -> {
            User patientUser = userRepository.findByEmail("patient@example.com").orElseGet(() -> {
                User user = new User();
                user.setEmail("patient@example.com");
                user.setPassword("patient123");
                user.setRole(Role.PATIENT);
                return userRepository.save(user);
            });

            if (patientRepository.findByUser_Email("patient@example.com").isEmpty()) {
                PatientDao patient = new PatientDao();
                patient.setName("Demo Patient");
                patient.setDateOfBirth(LocalDate.of(1995, 6, 14));
                patient.setUser(patientUser);
                patientRepository.save(patient);
            }

            if (doctorRepository.count() == 0) {
                User doctorUser1 = userRepository.findByEmail("sara@example.com").orElseGet(() -> {
                    User user = new User();
                    user.setEmail("sara@example.com");
                    user.setPassword("doctor123");
                    user.setRole(Role.DOCTOR);
                    return userRepository.save(user);
                });

                DoctorDao doctor1 = new DoctorDao();
                doctor1.setName("Dr. Sara Ali");
                doctor1.setAddress("Clinic Street 12");
                doctor1.setSpecialization("Dermatology");
                doctor1.setUser(doctorUser1);
                doctor1 = doctorRepository.save(doctor1);

                DoctorServiceDao skinCheck = new DoctorServiceDao();
                skinCheck.setDoctor(doctor1);
                skinCheck.setName("Skin Check");
                skinCheck.setDescription("General dermatology consultation");
                skinCheck.setDurationMinutes(30);
                skinCheck.setPrice(new BigDecimal("59.00"));
                doctorServiceRepository.save(skinCheck);

                DoctorServiceDao followUp = new DoctorServiceDao();
                followUp.setDoctor(doctor1);
                followUp.setName("Follow-up Visit");
                followUp.setDescription("Follow-up after treatment");
                followUp.setDurationMinutes(20);
                followUp.setPrice(new BigDecimal("35.00"));
                doctorServiceRepository.save(followUp);

                User doctorUser2 = userRepository.findByEmail("karim@example.com").orElseGet(() -> {
                    User user = new User();
                    user.setEmail("karim@example.com");
                    user.setPassword("doctor123");
                    user.setRole(Role.DOCTOR);
                    return userRepository.save(user);
                });

                DoctorDao doctor2 = new DoctorDao();
                doctor2.setName("Dr. Karim Hassan");
                doctor2.setAddress("Health Center Road 7");
                doctor2.setSpecialization("General Medicine");
                doctor2.setUser(doctorUser2);
                doctor2 = doctorRepository.save(doctor2);

                DoctorServiceDao consult = new DoctorServiceDao();
                consult.setDoctor(doctor2);
                consult.setName("General Consultation");
                consult.setDescription("Primary care consultation");
                consult.setDurationMinutes(30);
                consult.setPrice(new BigDecimal("49.00"));
                doctorServiceRepository.save(consult);
            }

            if (allergyRepository.count() == 0) {
                AllergyDao penicillin = new AllergyDao();
                penicillin.setName("Penicillin");
                penicillin.setDescription("Allergy to penicillin antibiotics");
                allergyRepository.save(penicillin);

                AllergyDao peanuts = new AllergyDao();
                peanuts.setName("Peanuts");
                peanuts.setDescription("Peanut allergy");
                allergyRepository.save(peanuts);

                AllergyDao latex = new AllergyDao();
                latex.setName("Latex");
                latex.setDescription("Latex sensitivity");
                allergyRepository.save(latex);

                AllergyDao pollen = new AllergyDao();
                pollen.setName("Pollen");
                pollen.setDescription("Seasonal pollen allergy");
                allergyRepository.save(pollen);
            }
        };
    }
}