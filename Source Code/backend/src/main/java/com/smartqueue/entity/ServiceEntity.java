package com.smartqueue.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a type of service offered (e.g. "General Consultation", "Lab Test").
 *
 * Named ServiceEntity to avoid clashing with Spring's @Service annotation at
 * the class-name level. The database table is named "services" and all business
 * logic method names (createService, updateService, etc.) are preserved in
 * AdminService.
 *
 * UML Class Diagram attributes: serviceID, serviceName, description,
 *                               duration, isAvailable
 */
@Entity
@Table(name = "services")
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int duration;          // expected session length in minutes

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable = true;

    // One Service → many Appointments
    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Appointment> appointments = new ArrayList<>();

    // One Service → many Queues (one per day in practice)
    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ServiceQueue> queues = new ArrayList<>();

    public ServiceEntity() {}

    public ServiceEntity(String serviceName, String description, int duration) {
        this.serviceName = serviceName;
        this.description = description;
        this.duration    = duration;
        this.isAvailable = true;
    }

    /**
     * Checks whether there is room to book at a given date/time.
     * Preserved from UML Class Diagram: checkAvailability(serviceID, date, timeSlot) : Boolean
     * In the web app, BookingService also checks the appointments table.
     */
    public boolean checkAvailability() {
        return this.isAvailable;
    }

    // ---- Getters & Setters ----
    public Long   getId()          { return id; }
    public String getServiceName() { return serviceName; }
    public String getDescription() { return description; }
    public int    getDuration()    { return duration; }
    public boolean isAvailable()   { return isAvailable; }
    public List<Appointment>  getAppointments() { return appointments; }
    public List<ServiceQueue> getQueues()        { return queues; }

    public void setId(Long id)                   { this.id = id; }
    public void setServiceName(String n)         { this.serviceName = n; }
    public void setDescription(String d)         { this.description = d; }
    public void setDuration(int d)               { this.duration = d; }
    public void setAvailable(boolean a)          { this.isAvailable = a; }

    @Override
    public String toString() {
        return serviceName + " (" + duration + " min)";
    }
}
