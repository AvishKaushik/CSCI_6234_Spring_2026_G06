package com.smartqueue.repository;

import com.smartqueue.entity.Appointment;
import com.smartqueue.entity.Customer;
import com.smartqueue.entity.ServiceEntity;
import com.smartqueue.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByCustomerOrderByDateDescTimeSlotDesc(Customer customer);

    List<Appointment> findByServiceAndDate(ServiceEntity service, LocalDate date);

    // Slot availability: is this service slot already taken?
    boolean existsByServiceAndDateAndTimeSlotAndStatusNot(
        ServiceEntity service, LocalDate date, LocalTime timeSlot, AppointmentStatus status);

    // Customer overlap: does this customer already have something at this exact time?
    boolean existsByCustomerAndDateAndTimeSlotAndStatusNot(
        Customer customer, LocalDate date, LocalTime timeSlot, AppointmentStatus status);

    Optional<Appointment> findFirstByServiceAndStatus(ServiceEntity service, AppointmentStatus status);

    // For appointment-priority in queue: get today's confirmed appointments for a service
    List<Appointment> findByServiceAndDateAndStatus(ServiceEntity service, LocalDate date, AppointmentStatus status);

    // Find a specific customer's appointment for a service on a date
    Optional<Appointment> findByCustomerAndServiceAndDateAndStatus(
        Customer customer, ServiceEntity service, LocalDate date, AppointmentStatus status);
}
