package com.smartqueue.controller.api;

import com.smartqueue.entity.*;
import com.smartqueue.repository.CustomerRepository;
import com.smartqueue.repository.StaffRepository;
import com.smartqueue.service.AdminService;
import com.smartqueue.service.BookingService;
import com.smartqueue.service.QueueManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Robustness Diagram: Queue Controller
 *
 * Handles UC4 — Join Virtual Queue and UC7 — Call Next Customer & Complete Service.
 *
 * Diagram flows:
 *   JoinVirtualQueue:
 *     QueueUI → QueueController.requestJoin()
 *             → QueueEntity.getCurrentQueue(serviceID)
 *             → QueueEntity.addCustomerToQueue(customerID, serviceID)
 *             → QueueEntity.updateQueuePosition(queueID, customerID)
 *             → QueueUI: displayQueueNumber(queueNumber)
 *
 *   CallCustomerAndCompleteService:
 *     QueueUI → QueueController.requestNextCustomer()
 *             → AppointmentEntity.getNextAppointment(serviceID)
 *             → QueueEntity.getNextQueueCustomer(serviceID)
 *             → QueueEntity.updateQueueOrder(queueID)
 *             → AppointmentEntity.updateStatus("In Service")
 *             → QueueUI: displayCustomerDetails(customerID, queueNumber)
 */
@RestController
public class QueueController {

    private final CustomerRepository     customerRepo;
    private final StaffRepository        staffRepository;
    private final AdminService           adminService;
    private final QueueManagementService queueService;
    private final BookingService         bookingService;

    public QueueController(CustomerRepository customerRepo,
                            StaffRepository staffRepository,
                            AdminService adminService,
                            QueueManagementService queueService,
                            BookingService bookingService) {
        this.customerRepo    = customerRepo;
        this.staffRepository = staffRepository;
        this.adminService    = adminService;
        this.queueService    = queueService;
        this.bookingService  = bookingService;
    }

    private Customer resolveCustomer(UserDetails ud) {
        return customerRepo.findByEmail(ud.getUsername()).orElse(null);
    }

    // -----------------------------------------------------------------------
    //  Customer side — JoinVirtualQueue (UC4)
    // -----------------------------------------------------------------------

    /**
     * Diagram: QueueUI → QueueController.requestJoin(customerID, serviceID)
     *          → QueueEntity.getCurrentQueue(serviceID)
     *          → QueueEntity.addCustomerToQueue(customerID, serviceID)
     *          → QueueEntity.updateQueuePosition(queueID, customerID)
     *          → QueueUI: displayQueueNumber(queueNumber)
     */
    @PostMapping("/api/customer/queue/join")
    public ResponseEntity<?> requestJoin(@AuthenticationPrincipal UserDetails ud,
                                          @RequestBody Map<String, Long> body) {
        Customer customer = resolveCustomer(ud);
        if (customer == null) return ResponseEntity.status(401).build();

        ServiceEntity service = adminService.findService(body.get("serviceId")).orElse(null);
        if (service == null) return ResponseEntity.badRequest().body(Map.of("error", "Service not found"));

        // QueueEntity.getCurrentQueue(serviceID)
        ServiceQueue queue = getCurrentQueue(service);

        // QueueEntity.addCustomerToQueue(customerID, serviceID)
        QueueEntry entry = addCustomerToQueue(customer, service);
        if (entry == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Could not join queue — you may already be waiting"));
        }

        // QueueEntity.updateQueuePosition(queueID, customerID)
        updateQueuePosition(queue);

        // QueueUI: displayQueueNumber(queueNumber)
        return ResponseEntity.ok(Map.of(
            "id",          entry.getId(),
            "serviceName", service.getServiceName(),
            "queueNumber", entry.getQueueNumber(),
            "position",    entry.getPosition(),
            "status",      entry.getStatus().toString()
        ));
    }

    /** Returns the customer's own queue entries (all services, all statuses). */
    @GetMapping("/api/customer/queue")
    public ResponseEntity<?> getCurrentQueue(@AuthenticationPrincipal UserDetails ud) {
        Customer customer = resolveCustomer(ud);
        if (customer == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(
            queueService.getCustomerEntries(customer).stream().map(e -> Map.of(
                "id",          e.getId(),
                "serviceName", e.getQueue().getService().getServiceName(),
                "queueNumber", e.getQueueNumber(),
                "position",    e.getPosition(),
                "status",      e.getStatus().toString(),
                "joinedAt",    e.getJoinedAt().toString()
            )).toList()
        );
    }

    // -----------------------------------------------------------------------
    //  Staff side — CallCustomerAndCompleteService (UC7)
    // -----------------------------------------------------------------------

    /**
     * Returns services assigned to the logged-in staff member with their waiting counts.
     * Diagram: QueueUI displayQueueNumber — staff sees waiting count per service.
     */
    @Transactional(readOnly = true)
    @GetMapping("/api/staff/services")
    public ResponseEntity<?> displayQueueNumber(@AuthenticationPrincipal UserDetails ud) {
        Staff staff = staffRepository.findByEmail(ud.getUsername()).orElse(null);
        if (staff == null) return ResponseEntity.status(401).build();

        Set<ServiceEntity> assigned = staff.getAssignedServices();
        List<ServiceEntity> services = assigned.isEmpty()
            ? adminService.getAvailableServices()
            : assigned.stream().filter(ServiceEntity::isAvailable).collect(Collectors.toList());

        return ResponseEntity.ok(
            services.stream().map(s -> Map.of(
                "id",           s.getId(),
                "serviceName",  s.getServiceName(),
                "waitingCount", queueService.getCurrentQueue(s)
            )).toList()
        );
    }

    /**
     * Returns all queue entries for a service.
     * Diagram: QueueController → QueueEntity.getNextQueueCustomer(serviceID)
     */
    @GetMapping("/api/staff/queue/{serviceId}")
    public ResponseEntity<?> getNextQueueCustomer(@PathVariable Long serviceId) {
        ServiceEntity service = adminService.findService(serviceId).orElse(null);
        if (service == null) return ResponseEntity.notFound().build();

        ServiceQueue queue = getCurrentQueue(service);
        List<QueueEntry> all = queueService.getAllEntries(queue);
        Set<Long> appointmentCustomerIds = queueService.getTodayAppointmentCustomerIds(service);

        return ResponseEntity.ok(
            all.stream().map(e -> Map.of(
                "id",             e.getId(),
                "customerName",   e.getCustomer().getName(),
                "queueNumber",    e.getQueueNumber(),
                "position",       e.getPosition(),
                "status",         e.getStatus().toString(),
                "vip",            e.isVIP(),
                "hasAppointment", appointmentCustomerIds.contains(e.getCustomer().getId())
            )).toList()
        );
    }

    /**
     * Diagram: QueueUI → QueueController.requestNextCustomer(serviceID)
     *          → AppointmentEntity.getNextAppointment(serviceID)
     *          → QueueEntity.getNextQueueCustomer(serviceID)
     *          → QueueEntity.updateQueueOrder(queueID)
     *          → AppointmentEntity.updateStatus("In Service")
     *          → QueueUI: displayCustomerDetails(customerID, queueNumber)
     *
     * Blocks with 409 if a customer is already being served.
     */
    @PostMapping("/api/staff/queue/{serviceId}/call-next")
    public ResponseEntity<?> requestNextCustomer(@PathVariable Long serviceId) {
        ServiceEntity service = adminService.findService(serviceId).orElse(null);
        if (service == null) return ResponseEntity.notFound().build();

        ServiceQueue queue = getCurrentQueue(service);
        if (queueService.hasActiveEntry(queue)) {
            return ResponseEntity.status(409).body(Map.of(
                "error", "A customer is already being served. Complete or mark no-show first."));
        }

        // AppointmentEntity.getNextAppointment(serviceID) — checks for due appointments
        getNextAppointment(service);

        // QueueEntity.getNextQueueCustomer(serviceID) — peeks at next waiting customer
        getNextQueueCustomer(service);

        // Calls next + QueueEntity.updateQueueOrder(queueID) + AppointmentEntity.updateStatus("In Service")
        QueueEntry entry = queueService.requestNextCustomer(service);
        if (entry == null) return ResponseEntity.ok(Map.of("message", "No customers waiting"));

        // QueueEntity.updateQueueOrder(queueID)
        updateQueueOrder(queue);

        // QueueUI: displayCustomerDetails(customerID, queueNumber)
        return ResponseEntity.ok(displayCustomerDetails(entry, service));
    }

    /**
     * Diagram: Queue.completeService(QueueEntry entry)
     * Marks the current IN_SERVICE entry as COMPLETED and updates appointment status.
     */
    @PostMapping("/api/staff/entries/{entryId}/complete")
    public ResponseEntity<?> completeService(@PathVariable Long entryId) {
        QueueEntry entry = queueService.findEntry(entryId);
        if (entry == null) return ResponseEntity.notFound().build();
        queueService.completeService(entry);
        return ResponseEntity.ok(Map.of("message", "Service completed"));
    }

    /** Marks a WAITING entry as NO_SHOW and updates appointment status. */
    @PostMapping("/api/staff/entries/{entryId}/no-show")
    public ResponseEntity<?> markNoShow(@PathVariable Long entryId) {
        QueueEntry entry = queueService.findEntry(entryId);
        if (entry == null) return ResponseEntity.notFound().build();
        queueService.markNoShow(entry);
        return ResponseEntity.ok(Map.of("message", "Marked as no-show"));
    }

    // -----------------------------------------------------------------------
    //  Diagram entity methods — QueueEntity
    // -----------------------------------------------------------------------

    /**
     * Diagram: QueueEntity.getCurrentQueue(serviceID)
     * Gets or creates today's queue for the given service.
     */
    private ServiceQueue getCurrentQueue(ServiceEntity service) {
        return queueService.getOrCreateQueue(service, LocalDate.now());
    }

    /**
     * Diagram: QueueEntity.addCustomerToQueue(customerID, serviceID)
     * Adds the customer to today's queue for the service; returns null if already waiting.
     */
    private QueueEntry addCustomerToQueue(Customer customer, ServiceEntity service) {
        return queueService.requestJoin(customer, service);
    }

    /**
     * Diagram: QueueEntity.updateQueuePosition(queueID, customerID)
     * Recalculates positions for all WAITING entries after a join.
     */
    private void updateQueuePosition(ServiceQueue queue) {
        queueService.updateQueueOrder(queue);
    }

    /**
     * Diagram: QueueEntity.getNextQueueCustomer(serviceID)
     * Returns the email of the next WAITING customer in the queue.
     */
    private String getNextQueueCustomer(ServiceEntity service) {
        return queueService.getNextQueueCustomer(service);
    }

    /**
     * Diagram: QueueEntity.updateQueueOrder(queueID)
     * Recalculates positions after a customer is called or served.
     */
    private void updateQueueOrder(ServiceQueue queue) {
        queueService.updateQueueOrder(queue);
    }

    /**
     * Diagram: QueueUI: displayCustomerDetails(customerID, queueNumber)
     * Builds the response map shown to staff after calling next customer.
     */
    private Map<String, Object> displayCustomerDetails(QueueEntry entry, ServiceEntity service) {
        boolean hadAppointment = queueService.getTodayAppointmentCustomerIds(service)
                                             .contains(entry.getCustomer().getId());
        return Map.of(
            "id",             entry.getId(),
            "customerName",   entry.getCustomer().getName(),
            "queueNumber",    entry.getQueueNumber(),
            "status",         entry.getStatus().toString(),
            "hasAppointment", hadAppointment
        );
    }

    // -----------------------------------------------------------------------
    //  Diagram entity methods — AppointmentEntity
    // -----------------------------------------------------------------------

    /**
     * Diagram: AppointmentEntity.getNextAppointment(serviceID)
     * Returns the next CONFIRMED appointment for the service (used for priority check).
     */
    private Appointment getNextAppointment(ServiceEntity service) {
        return bookingService.getNextAppointment(service);
    }
}
