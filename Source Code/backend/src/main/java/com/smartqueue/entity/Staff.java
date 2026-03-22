package com.smartqueue.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Staff entity — calls customers and completes service sessions.
 * Discriminator value "STAFF" in the users table.
 * Staff members are assigned to services by an Admin.
 */
@Entity
@DiscriminatorValue("STAFF")
public class Staff extends User {

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "staff_services",
        joinColumns = @JoinColumn(name = "staff_id"),
        inverseJoinColumns = @JoinColumn(name = "service_id"))
    private Set<ServiceEntity> assignedServices = new HashSet<>();

    public Staff() {}

    public Staff(String name, String email, String password) {
        super(name, email, password);
    }

    @Override
    public String getRole() { return "STAFF"; }

    public Set<ServiceEntity> getAssignedServices() { return assignedServices; }
    public void setAssignedServices(Set<ServiceEntity> services) { this.assignedServices = services; }
}
