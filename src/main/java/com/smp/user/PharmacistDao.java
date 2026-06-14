package com.smp.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pharmacists")
@Getter
@Setter
@NoArgsConstructor
public class PharmacistDao extends User {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @PrePersist
    private void ensurePharmacistRole() {
        if (getRole() == null) {
            setRole(Role.PHARMACIST);
        }
    }
}