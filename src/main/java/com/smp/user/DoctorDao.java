package com.smp.user;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.smp.appointment.AppointmentDao;
import com.smp.appointment.DoctorAvailabilityDao;
import com.smp.appointment.DoctorServiceDao;
import com.smp.appointment.DoctorTimeOffDao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
public class DoctorDao  {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String specialization;

    @Column(nullable = false)
    private boolean active;

       @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

       @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true)
       private Set<DoctorServiceDao> services = new HashSet<>();

       @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true)
       private Set<DoctorAvailabilityDao> availabilities = new HashSet<>();

       @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true)
       private Set<DoctorTimeOffDao> timeOffs = new HashSet<>();

       @OneToMany(mappedBy = "doctor")
       private Set<AppointmentDao> appointments = new HashSet<>();

       @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


}
