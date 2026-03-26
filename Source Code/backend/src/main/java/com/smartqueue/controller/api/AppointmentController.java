package com.smartqueue.controller.api;

import com.smartqueue.entity.*;
import com.smartqueue.enums.AppointmentStatus;
import com.smartqueue.enums.QueueStatus;
import com.smartqueue.repository.AppointmentRepository;
import com.smartqueue.repository.CustomerRepository;
import com.smartqueue.service.AdminService;
import com.smartqueue.service.BookingService;
import com.smartqueue.service.QueueManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Robustness Diagram: Appointment Controller
 *
 * Handles UC2 — Book Appointment and UC3 — Cancel Appointment.
 *
 * Diagram flows:
 *   BookAppointment:    BookingUI → AppointmentController.submitBooking()
 *                                 → ServiceEntity.checkAvailability()
 *                                 → AppointmentEntity.createAppointment()
 *                                 → BookingUI: confirmation / error
 *
 *   CancelAppointment:  CancelUI → AppointmentController.requestCancellation()
 *                                → AppointmentEntity.findAppointment()
 *                                → AppointmentEntity.updateStatus("Cancelled")
 *                                → CancelUI: cancellationConfirmation / error
 */
@RestController
@RequestMapping("/api/customer")
public class AppointmentController {

    private static final List<String> ALL_SLOTS = Arrays.asList(
        "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
        "13:00", "13:30", "14:00", "14:30", "15:00", "15:30", "16:00"
    );

    private final CustomerRepository     customerRepo;
    private final AdminService           adminService;
    private final BookingService         bookingService;
    private final QueueManagementService queueService;
    private final AppointmentRepository  appointmentRepository;

    public AppointmentController(CustomerRepository customerRepo,
                                  AdminService adminService,
                                  BookingService bookingService,
                                  QueueManagementService queueService,
                                  AppointmentRepository appointmentRepository) {
        this.customerRepo          = customerRepo;
        this.adminService          = adminService;
        this.bookingService        = bookingService;
        this.queueService          = queueService;
        this.appointmentRepository = appointmentRepository;
    }

    private Customer resolveCustomer(UserDetails ud) {
        return customerRepo.findByEmail(ud.getUsername()).orElse(null);
    }

    // ---- Dashboard ----

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(@AuthenticationPrincipal UserDetails ud) {
        Customer customer = resolveCustomer(ud);
        if (customer == null) return ResponseEntity.status(401).build();

        List<Appointment> appointments = bookingService.getCustomerAppointments(customer);
        long confirmed = appointments.stream()
            .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count();

        List<QueueEntry> entries = queueService.getCustomerEntries(customer);
        long activeQueues = entries.stream()
            .filter(e -> e.getStatus() == QueueStatus.WAITING
                      || e.getStatus() == QueueStatus.CALLED
                      || e.getStatus() == QueueStatus.IN_SERVICE)
            .count();

        return ResponseEntity.ok(Map.of(
            "confirmedAppointments", confirmed,
            "totalAppointments",     appointments.size(),
            "activeQueues",          activeQueues
        ));
    }

    // ---- Services (for booking) ----

    @GetMapping("/services")
    public ResponseEntity<?> services() {
        return ResponseEntity.ok(
            adminService.getAvailableServices().stream().map(s -> Map.of(
                "id",          s.getId(),
                "serviceName", s.getServiceName(),
                "description", s.getDescription() == null ? "" : s.getDescription(),
                "duration",    s.getDuration()
            )).toList()
        );
    }

    /**
     * Returns available (un-booked) time slots for a service on a given date.
     * Filters out past slots when date is today.
     */
    @GetMapping("/services/{serviceId}/available-slots")
    public ResponseEntity<?> availableSlots(@PathVariable Long serviceId,
                                             @RequestParam String date) {
        ServiceEntity service = adminService.findService(serviceId).orElse(null);
        if (service == null) return ResponseEntity.notFound().build();

        LocalDate d = LocalDate.parse(date);
        Set<String> taken = appointmentRepository.findByServiceAndDate(service, d)
            .stream()
            .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
            .map(a -> a.getTimeSlot().toString())
            .collect(Collectors.toSet());

        LocalTime now = LocalTime.now();
        List<String> available = ALL_SLOTS.stream()
            .filter(s -> !taken.contains(s))
            .filter(s -> !d.isEqual(LocalDate.now()) || LocalTime.parse(s).isAfter(now))
            .collect(Collectors.toList());

        return ResponseEntity.ok(available);
    }

    // ---- Appointments list ----

    @GetMapping("/appointments")
    public ResponseEntity<?> appointments(@AuthenticationPrincipal UserDetails ud) {
        Customer customer = resolveCustomer(ud);
        if (customer == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(
            bookingService.getCustomerAppointments(customer).stream().map(a -> Map.of(
                "id",          a.getId(),
                "serviceName", a.getService().getServiceName(),
                "date",        a.getDate().toString(),
                "timeSlot",    a.getTimeSlot().toString(),
                "status",      a.getStatus().toString()
            )).toList()
        );
    }

    /**
     * Diagram: BookingUI → AppointmentController.submitBooking(customerID, serviceID, date, timeSlot)
     *          → ServiceEntity.checkAvailability(serviceID, date, timeSlot)
     *          → AppointmentEntity.createAppointment(customerID, serviceID, date, timeSlot)
     *          → BookingUI: confirmation(appointmentID) / error(message)
     */
    @PostMapping("/appointments")
    public ResponseEntity<?> submitBooking(@AuthenticationPrincipal UserDetails ud,
                                            @RequestBody Map<String, String> body) {
        Customer customer = resolveCustomer(ud);
        if (customer == null) return ResponseEntity.status(401).build();

        Long serviceId = Long.parseLong(body.get("serviceId"));
        LocalDate date = LocalDate.parse(body.get("date"));
        LocalTime time = LocalTime.parse(body.get("timeSlot"));

        ServiceEntity service = adminService.findService(serviceId).orElse(null);
        if (service == null) return ResponseEntity.badRequest().body(Map.of("error", "Service not found"));

        // ServiceEntity.checkAvailability() + AppointmentEntity.createAppointment()
        Appointment appt = createAppointment(customer, service, date, time);
        if (appt == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Time slot unavailable"));
        }
        return ResponseEntity.ok(Map.of(
            "id",          appt.getId(),
            "serviceName", appt.getService().getServiceName(),
            "date",        appt.getDate().toString(),
            "timeSlot",    appt.getTimeSlot().toString(),
            "status",      appt.getStatus().toString()
        ));
    }

    /**
     * Diagram: CancelUI → AppointmentController.requestCancellation(appointmentID)
     *          → AppointmentEntity.findAppointment(appointmentID)
     *          → AppointmentEntity.updateStatus(appointmentID, "Cancelled")
     *          → CancelUI: cancellationConfirmation(appointmentID) / error(message)
     */
    @DeleteMapping("/appointments/{id}")
    public ResponseEntity<?> requestCancellation(@AuthenticationPrincipal UserDetails ud,
                                                  @PathVariable Long id) {
        Customer customer = resolveCustomer(ud);
        if (customer == null) return ResponseEntity.status(401).build();

        // AppointmentEntity.findAppointment(appointmentID)
        Appointment appt = findAppointment(id);
        if (appt == null) return ResponseEntity.badRequest().body(Map.of("error", "Cannot cancel this appointment"));

        // AppointmentEntity.updateStatus(appointmentID, "Cancelled")
        boolean cancelled = updateStatus(id, customer);
        if (!cancelled) return ResponseEntity.badRequest().body(Map.of("error", "Cannot cancel this appointment"));
        return ResponseEntity.ok(Map.of("message", "Appointment cancelled"));
    }

    // -----------------------------------------------------------------------
    //  Diagram entity methods — AppointmentEntity
    // -----------------------------------------------------------------------

    /**
     * Diagram: AppointmentEntity.createAppointment(customerID, serviceID, date, timeSlot)
     * Checks slot availability then creates and persists the appointment.
     */
    private Appointment createAppointment(Customer customer, ServiceEntity service,
                                           LocalDate date, LocalTime time) {
        return bookingService.submitBooking(customer, service, date, time);
    }

    /**
     * Diagram: AppointmentEntity.findAppointment(appointmentID)
     * Retrieves an appointment by ID.
     */
    private Appointment findAppointment(Long id) {
        return bookingService.findAppointment(id);
    }

    /**
     * Diagram: AppointmentEntity.updateStatus(appointmentID, "Cancelled")
     * Cancels a CONFIRMED appointment owned by the given customer.
     */
    private boolean updateStatus(Long id, Customer customer) {
        return bookingService.requestCancellation(id, customer);
    }
}
