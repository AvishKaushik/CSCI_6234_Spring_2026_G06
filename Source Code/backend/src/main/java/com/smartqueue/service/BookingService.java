package com.smartqueue.service;

import com.smartqueue.entity.Appointment;
import com.smartqueue.entity.Customer;
import com.smartqueue.entity.ServiceEntity;
import com.smartqueue.enums.AppointmentStatus;
import com.smartqueue.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Handles UC2 (Book Appointment) and UC3 (Cancel Appointment).
 *
 * Method names preserved from UML sequence diagrams:
 *   submitBooking     → AppointmentController.submitBooking(...)
 *   requestCancellation → AppointmentController.requestCancellation(...)
 *   createAppointment → Appointment.createAppointment(...)
 *   findAppointment   → Appointment.findAppointment(...)
 *   getNextAppointment → Appointment.getNextAppointment(...)
 *   updateStatus      → Appointment.updateStatus(...)
 *   checkAvailability → Service.checkAvailability(...)
 */
@Service
public class BookingService {

    private final AppointmentRepository appointmentRepository;
    private final NotificationService   notificationService;

    public BookingService(AppointmentRepository appointmentRepository,
                          NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.notificationService   = notificationService;
    }

    // -----------------------------------------------------------------------
    //  UC2 — Book Appointment
    // -----------------------------------------------------------------------

    /**
     * Validates availability and creates an appointment.
     *
     * Flow (sequence diagram):
     *   submitBooking → checkAvailability → createAppointment → notify
     *
     * @return the created Appointment, or null if the slot is taken
     */
    @Transactional
    public Appointment submitBooking(Customer customer, ServiceEntity service,
                                     LocalDate date, LocalTime timeSlot) {
        // checkAvailability — service must be active and slot not already taken
        if (!service.checkAvailability()) return null;
        if (!isSlotAvailable(service, date, timeSlot)) return null;
        // Fix 5: customer must not already have an appointment at this exact time
        if (hasCustomerConflict(customer, date, timeSlot)) return null;

        // createAppointment
        Appointment appointment = createAppointment(customer, service, date, timeSlot);

        // Notify customer
        notificationService.notifyCustomer(
            customer.getName(), customer.getPhoneNumber(),
            "Appointment confirmed for " + service.getServiceName()
                + " on " + date + " at " + timeSlot);

        return appointment;
    }

    /**
     * Creates and persists a new appointment.
     * Diagram: Appointment.createAppointment(customerID, serviceID, date, timeSlot)
     */
    @Transactional
    public Appointment createAppointment(Customer customer, ServiceEntity service,
                                          LocalDate date, LocalTime timeSlot) {
        Appointment appointment = new Appointment(customer, service, date, timeSlot);
        return appointmentRepository.save(appointment);
    }

    /**
     * Checks whether a specific slot is still available for a service.
     * Diagram: Service.checkAvailability(serviceID, date, timeSlot) : Boolean
     */
    public boolean isSlotAvailable(ServiceEntity service, LocalDate date, LocalTime timeSlot) {
        return !appointmentRepository.existsByServiceAndDateAndTimeSlotAndStatusNot(
            service, date, timeSlot, AppointmentStatus.CANCELLED);
    }

    /** Fix 5: Prevent customer from booking two things at the exact same time. */
    public boolean hasCustomerConflict(Customer customer, LocalDate date, LocalTime timeSlot) {
        return appointmentRepository.existsByCustomerAndDateAndTimeSlotAndStatusNot(
            customer, date, timeSlot, AppointmentStatus.CANCELLED);
    }

    // -----------------------------------------------------------------------
    //  UC3 — Cancel Appointment
    // -----------------------------------------------------------------------

    /**
     * Cancels a CONFIRMED appointment.
     *
     * Flow: requestCancellation → findAppointment → updateStatus(CANCELLED) → notify
     *
     * @return true if cancelled, false if not found or not cancellable
     */
    @Transactional
    public boolean requestCancellation(Long appointmentId, Customer customer) {
        Appointment appointment = findAppointment(appointmentId);
        if (appointment == null) return false;

        // Only the owning customer can cancel
        if (!appointment.getCustomer().getId().equals(customer.getId())) return false;

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) return false;

        // updateStatus
        updateStatus(appointment, AppointmentStatus.CANCELLED);

        notificationService.notifyCustomer(
            customer.getName(), customer.getPhoneNumber(),
            "Your appointment for " + appointment.getService().getServiceName()
                + " on " + appointment.getDate() + " has been cancelled.");

        return true;
    }

    /**
     * Diagram: Appointment.findAppointment(appointmentID) : Appointment
     */
    public Appointment findAppointment(Long id) {
        return appointmentRepository.findById(id).orElse(null);
    }

    /**
     * Returns the next CONFIRMED appointment for a service.
     * Diagram: Appointment.getNextAppointment(serviceID) : Appointment
     */
    public Appointment getNextAppointment(ServiceEntity service) {
        return appointmentRepository
            .findFirstByServiceAndStatus(service, AppointmentStatus.CONFIRMED)
            .orElse(null);
    }

    /**
     * Transitions an appointment to a new status.
     * Diagram: Appointment.updateStatus(appointmentID, status) : void
     */
    @Transactional
    public void updateStatus(Appointment appointment, AppointmentStatus newStatus) {
        appointment.updateStatus(newStatus);
        appointmentRepository.save(appointment);
    }

    /**
     * Returns all appointments for a customer, newest first.
     */
    public List<Appointment> getCustomerAppointments(Customer customer) {
        return appointmentRepository.findByCustomerOrderByDateDescTimeSlotDesc(customer);
    }
}
