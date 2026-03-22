package com.smartqueue.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer entity — can book appointments and join virtual queues.
 * Discriminator value "CUSTOMER" in the users table.
 */
@Entity
@DiscriminatorValue("CUSTOMER")
public class Customer extends User {

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    // One Customer → many Appointments (historical record)
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Appointment> appointments = new ArrayList<>();

    // One Customer → many QueueEntries (historical record)
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QueueEntry> queueEntries = new ArrayList<>();

    public Customer() {}

    public Customer(String name, String email, String password, String phoneNumber) {
        super(name, email, password);
        this.phoneNumber      = phoneNumber;
        this.registrationDate = LocalDate.now();
    }

    /**
     * Convenience method preserved from spec: Customer.bookAppointment()
     * In the web app, appointment creation is handled by BookingService;
     * this method records the appointment in the customer's local list.
     */
    public void addAppointment(Appointment appointment) {
        appointments.add(appointment);
    }

    @Override
    public String getRole() { return "CUSTOMER"; }

    // ---- Getters & Setters ----
    public String       getPhoneNumber()      { return phoneNumber; }
    public LocalDate    getRegistrationDate()  { return registrationDate; }
    public List<Appointment> getAppointments(){ return appointments; }
    public List<QueueEntry>  getQueueEntries() { return queueEntries; }

    public void setPhoneNumber(String p)           { this.phoneNumber = p; }
    public void setRegistrationDate(LocalDate d)   { this.registrationDate = d; }
}
