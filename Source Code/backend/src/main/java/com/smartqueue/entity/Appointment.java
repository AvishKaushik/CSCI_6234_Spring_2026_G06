package com.smartqueue.entity;

import com.smartqueue.enums.AppointmentStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Links one Customer to one ServiceEntity at a specific date and time slot.
 *
 * UML Class Diagram methods: createAppointment, findAppointment,
 *                            getNextAppointment, updateStatus
 * These are implemented in BookingService, which acts as the controller
 * between the web layer and this entity.
 */
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "time_slot", nullable = false)
    private LocalTime timeSlot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Appointment() {}

    public Appointment(Customer customer, ServiceEntity service,
                       LocalDate date, LocalTime timeSlot) {
        this.customer  = customer;
        this.service   = service;
        this.date      = date;
        this.timeSlot  = timeSlot;
        this.status    = AppointmentStatus.CONFIRMED;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Convenience status updater.
     * Diagram: Appointment.updateStatus(appointmentID, status) : void
     */
    public void updateStatus(AppointmentStatus newStatus) {
        this.status = newStatus;
    }

    // ---- Getters & Setters ----
    public Long              getId()        { return id; }
    public Customer          getCustomer()  { return customer; }
    public ServiceEntity     getService()   { return service; }
    public LocalDate         getDate()      { return date; }
    public LocalTime         getTimeSlot()  { return timeSlot; }
    public AppointmentStatus getStatus()    { return status; }
    public LocalDateTime     getCreatedAt() { return createdAt; }

    public void setId(Long id)                        { this.id = id; }
    public void setCustomer(Customer c)               { this.customer = c; }
    public void setService(ServiceEntity s)           { this.service = s; }
    public void setDate(LocalDate d)                  { this.date = d; }
    public void setTimeSlot(LocalTime t)              { this.timeSlot = t; }
    public void setStatus(AppointmentStatus s)        { this.status = s; }
    public void setCreatedAt(LocalDateTime dt)        { this.createdAt = dt; }
}
