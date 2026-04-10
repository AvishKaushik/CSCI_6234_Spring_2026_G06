package com.smartqueue.service;

import com.smartqueue.entity.*;
import com.smartqueue.enums.AppointmentStatus;
import com.smartqueue.enums.QueueStatus;
import com.smartqueue.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Handles UC4 (Join Virtual Queue) and UC7 (Call Next Customer & Complete Service).
 *
 * Method names preserved from UML diagrams:
 *   requestJoin         → QueueController.requestJoin(customerID, serviceID)
 *   requestNextCustomer → QueueController.requestNextCustomer(staffID, serviceID)
 *   addEntry            → Queue.addEntry(Customer customer)
 *   callNext            → Queue.callNext()
 *   completeService     → Queue.completeService(QueueEntry entry)
 *   getCurrentQueue     → Queue.getCurrentQueue(serviceID)
 *   addCustomerToQueue  → Queue.addCustomerToQueue(customerID, serviceID)
 *   getNextQueueCustomer → Queue.getNextQueueCustomer(serviceID)
 *   updateQueueOrder    → Queue.updateQueueOrder(queueID)
 */
@Service
public class QueueManagementService {

    private final ServiceQueueRepository  queueRepository;
    private final QueueEntryRepository    entryRepository;
    private final AppointmentRepository   appointmentRepository;
    private final NotificationService     notificationService;

    public QueueManagementService(ServiceQueueRepository queueRepository,
                                  QueueEntryRepository   entryRepository,
                                  AppointmentRepository  appointmentRepository,
                                  NotificationService    notificationService) {
        this.queueRepository     = queueRepository;
        this.entryRepository     = entryRepository;
        this.appointmentRepository = appointmentRepository;
        this.notificationService  = notificationService;
    }

    // -----------------------------------------------------------------------
    //  UC4 — Join Virtual Queue
    // -----------------------------------------------------------------------

    /**
     * Handles a customer's request to join the virtual queue.
     *
     * Sequence diagram: VirtualQueueUI → QueueController.requestJoin(customerID, serviceID)
     *                                  → Queue.addEntry(customer)
     *
     * @return the created QueueEntry, or null if service is unavailable or customer already waiting
     */
    @Transactional
    public QueueEntry requestJoin(Customer customer, ServiceEntity service) {
        if (!service.checkAvailability()) return null;

        // Get or create today's queue for this service
        ServiceQueue queue = getOrCreateQueue(service, LocalDate.now());

        // Prevent duplicate active entries for the same customer
        if (isCustomerAlreadyWaiting(queue, customer)) return null;

        // addEntry — creates QueueEntry and adds to queue
        return addEntry(queue, customer);
    }

    /**
     * Creates a QueueEntry for a customer and adds it to the queue.
     * Diagram: Queue.addEntry(Customer customer)
     *
     * VIP customers jump ahead of non-VIP in position ordering.
     */
    @Transactional
    public QueueEntry addEntry(ServiceQueue queue, Customer customer) {
        int nextNumber = entryRepository.findByQueueOrderByQueueNumberAsc(queue)
            .stream().mapToInt(QueueEntry::getQueueNumber).max().orElse(0) + 1;

        int position = entryRepository.countWaiting(queue) + 1;

        QueueEntry entry = new QueueEntry(queue, customer, nextNumber, position);
        entryRepository.save(entry);

        // Convenience method from spec: addCustomerToQueue
        addCustomerToQueue(queue, entry);

        notificationService.notifyCustomer(
            customer.getName(), customer.getPhoneNumber(),
            "You joined the queue for " + queue.getService().getServiceName()
                + ". Queue #" + nextNumber + ", Position: " + position);

        return entry;
    }

    /**
     * Records the customer-to-queue association.
     * Diagram: Queue.addCustomerToQueue(customerID, serviceID) : Integer
     */
    private void addCustomerToQueue(ServiceQueue queue, QueueEntry entry) {
        queue.getEntries().add(entry);
    }

    // -----------------------------------------------------------------------
    //  UC7 — Call Next Customer & Complete Service
    // -----------------------------------------------------------------------

    /**
     * Staff calls the next WAITING customer for a service.
     *
     * Sequence diagram: StaffQueueUI → QueueController.requestNextCustomer(staffID, serviceID)
     *                               → Queue.callNext()
     *
     * @return the QueueEntry now IN_SERVICE, or null if queue is empty
     */
    @Transactional
    public QueueEntry requestNextCustomer(ServiceEntity service) {
        ServiceQueue queue = getOrCreateQueue(service, LocalDate.now());
        return callNext(queue);
    }

    /**
     * Moves the next WAITING entry → CALLED → IN_SERVICE.
     * Priority: appointment customers whose time has arrived (timeSlot <= now) sorted by
     * appointment time ascending; all others by queue join order (FIFO).
     * Diagram: Queue.callNext()
     */
    @Transactional
    public QueueEntry callNext(ServiceQueue queue) {
        // Auto-add appointment customers whose time has arrived but aren't in queue yet
        autoEnqueueDueAppointments(queue);

        List<QueueEntry> waiting = entryRepository
            .findByQueueAndStatusOrderByPositionAsc(queue, QueueStatus.WAITING);

        if (waiting.isEmpty()) return null;

        // Sort in Java — avoids relying on DB-flushed positions from updateQueueOrder
        LocalTime now = LocalTime.now();
        Map<Long, LocalTime> dueApptTimes = buildDueApptMap(queue, now);

        waiting.sort((a, b) -> {
            LocalTime ta = dueApptTimes.get(a.getCustomer().getId());
            LocalTime tb = dueApptTimes.get(b.getCustomer().getId());
            boolean aHasDue = ta != null;
            boolean bHasDue = tb != null;
            if (aHasDue != bHasDue) return aHasDue ? -1 : 1;
            if (aHasDue) return ta.compareTo(tb);   // earlier appointment first
            return Integer.compare(a.getQueueNumber(), b.getQueueNumber()); // FIFO
        });

        QueueEntry entry = waiting.get(0);

        // Mark VIP for display if this customer has a due appointment
        if (dueApptTimes.containsKey(entry.getCustomer().getId()) && !entry.isVIP()) {
            entry.setVIP(true);
        }

        entry.callCustomer();    // WAITING → CALLED
        entry.startService();    // CALLED  → IN_SERVICE
        entryRepository.save(entry);

        updateQueueOrder(queue);

        notificationService.notifyCustomer(entry,
            "You have been called! Please proceed to the service counter.");

        return entry;
    }

    /**
     * Auto-adds customers to the queue when their appointment time has arrived
     * and they are not already in the queue.
     * Called at the start of callNext() so appointment customers appear at the right moment.
     */
    @Transactional
    public void autoEnqueueDueAppointments(ServiceQueue queue) {
        LocalTime now = LocalTime.now();
        List<Appointment> due = appointmentRepository.findByServiceAndDateAndStatus(
            queue.getService(), LocalDate.now(), AppointmentStatus.CONFIRMED);

        for (Appointment appt : due) {
            if (!appt.getTimeSlot().isAfter(now)) {
                Customer customer = appt.getCustomer();
                if (!isCustomerAlreadyWaiting(queue, customer)) {
                    addEntry(queue, customer);
                }
            }
        }
    }

    /** Builds a map of customer ID → appointment time for confirmed appointments whose time has arrived. */
    private Map<Long, LocalTime> buildDueApptMap(ServiceQueue queue, LocalTime now) {
        return appointmentRepository
            .findByServiceAndDateAndStatus(queue.getService(), LocalDate.now(), AppointmentStatus.CONFIRMED)
            .stream()
            .filter(a -> !a.getTimeSlot().isAfter(now))
            .collect(Collectors.toMap(
                a -> a.getCustomer().getId(),
                Appointment::getTimeSlot,
                (t1, t2) -> t1.isBefore(t2) ? t1 : t2
            ));
    }

    /**
     * Returns true if a customer is currently CALLED or IN_SERVICE for this queue.
     * Used to block staff from calling next when someone is already being served.
     */
    public boolean hasActiveEntry(ServiceQueue queue) {
        return entryRepository.existsByQueueAndStatusIn(
            queue, Arrays.asList(QueueStatus.CALLED, QueueStatus.IN_SERVICE));
    }

    /**
     * Marks an IN_SERVICE entry as COMPLETED after staff finishes.
     * Also marks the customer's appointment as COMPLETED if one exists for today.
     * Diagram: Queue.completeService(QueueEntry entry)
     */
    @Transactional
    public void completeService(QueueEntry entry) {
        entry.completeService();   // IN_SERVICE → COMPLETED
        entryRepository.save(entry);

        updateQueueOrder(entry.getQueue());

        // Sync appointment status
        updateAppointmentStatus(entry, AppointmentStatus.COMPLETED);

        notificationService.notifyCustomer(entry,
            "Your service is complete. Thank you for using SmartQueue!");
    }

    /**
     * Marks a WAITING or CALLED entry as NO_SHOW.
     * Also marks the customer's appointment as NO_SHOW if one exists for today.
     */
    @Transactional
    public void markNoShow(QueueEntry entry) {
        entry.markNoShow();
        entryRepository.save(entry);
        updateQueueOrder(entry.getQueue());

        // Sync appointment status
        updateAppointmentStatus(entry, AppointmentStatus.NO_SHOW);
    }

    /** Updates the customer's CONFIRMED appointment for this service today to the given status. */
    private void updateAppointmentStatus(QueueEntry entry, AppointmentStatus newStatus) {
        ServiceEntity service = entry.getQueue().getService();
        Customer customer = entry.getCustomer();
        appointmentRepository
            .findByCustomerAndServiceAndDateAndStatus(customer, service, LocalDate.now(), AppointmentStatus.CONFIRMED)
            .ifPresent(appt -> {
                appt.updateStatus(newStatus);
                appointmentRepository.save(appt);
            });
    }

    // -----------------------------------------------------------------------
    //  Queue query / order helpers
    // -----------------------------------------------------------------------

    /**
     * Returns the count of WAITING customers for a service.
     * Diagram: Queue.getCurrentQueue(serviceID) : Integer
     */
    public int getCurrentQueue(ServiceEntity service) {
        ServiceQueue queue = queueRepository.findByServiceAndDate(service, LocalDate.now())
            .orElse(null);
        if (queue == null) return 0;
        return entryRepository.countWaiting(queue);
    }

    /**
     * Returns the customerID of the next WAITING entry.
     * Diagram: Queue.getNextQueueCustomer(serviceID) : String
     */
    public String getNextQueueCustomer(ServiceEntity service) {
        ServiceQueue queue = queueRepository.findByServiceAndDate(service, LocalDate.now())
            .orElse(null);
        if (queue == null) return null;
        return entryRepository.findFirstByQueueAndStatusOrderByPositionAsc(queue, QueueStatus.WAITING)
            .map(e -> e.getCustomer().getEmail())
            .orElse(null);
    }

    /**
     * Recalculates positions for all WAITING entries.
     * Priority order:
     *   1. Appointment customers whose time has arrived (timeSlot <= now), sorted by appointment time ascending
     *   2. Everyone else (walk-ins or future-appointment customers), sorted by queue join order (FIFO)
     *
     * Diagram: Queue.updateQueueOrder(queueID) : void
     */
    @Transactional
    public void updateQueueOrder(ServiceQueue queue) {
        List<QueueEntry> waiting = entryRepository
            .findByQueueAndStatusOrderByPositionAsc(queue, QueueStatus.WAITING);

        LocalTime now = LocalTime.now();
        Map<Long, LocalTime> dueApptTimes = buildDueApptMap(queue, now);

        waiting.sort((a, b) -> {
            LocalTime ta = dueApptTimes.get(a.getCustomer().getId());
            LocalTime tb = dueApptTimes.get(b.getCustomer().getId());
            boolean aHasDueAppt = ta != null;
            boolean bHasDueAppt = tb != null;
            // Appointment customers (time arrived) come first
            if (aHasDueAppt != bHasDueAppt) return aHasDueAppt ? -1 : 1;
            // Among appointment customers: earlier appointment time first
            if (aHasDueAppt) return ta.compareTo(tb);
            // Among walk-ins: first-come first-served (lower queue number = joined earlier)
            return Integer.compare(a.getQueueNumber(), b.getQueueNumber());
        });

        for (int i = 0; i < waiting.size(); i++) {
            waiting.get(i).setPosition(i + 1);
            entryRepository.save(waiting.get(i));
        }
    }

    /**
     * Gets or creates today's queue for a service.
     */
    @Transactional
    public ServiceQueue getOrCreateQueue(ServiceEntity service, LocalDate date) {
        return queueRepository.findByServiceAndDate(service, date)
            .orElseGet(() -> queueRepository.save(new ServiceQueue(service, date)));
    }

    /**
     * Returns all WAITING entries for a queue, sorted by position.
     */
    public List<QueueEntry> getWaitingEntries(ServiceQueue queue) {
        return entryRepository.findByQueueAndStatusOrderByPositionAsc(queue, QueueStatus.WAITING);
    }

    /**
     * Returns all entries for a queue (all statuses), for the status display.
     */
    public List<QueueEntry> getAllEntries(ServiceQueue queue) {
        return entryRepository.findByQueueOrderByQueueNumberAsc(queue);
    }

    /**
     * Finds a specific QueueEntry by ID.
     */
    public QueueEntry findEntry(Long entryId) {
        return entryRepository.findById(entryId).orElse(null);
    }

    /**
     * Returns queue entries belonging to a customer (all statuses, newest first).
     */
    public List<QueueEntry> getCustomerEntries(Customer customer) {
        return entryRepository.findByCustomerOrderByJoinedAtDesc(customer);
    }

    /** Returns IDs of customers who have a CONFIRMED appointment for this service today. */
    public java.util.Set<Long> getTodayAppointmentCustomerIds(ServiceEntity service) {
        return appointmentRepository
            .findByServiceAndDateAndStatus(service, LocalDate.now(), AppointmentStatus.CONFIRMED)
            .stream()
            .map(a -> a.getCustomer().getId())
            .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Checks whether a customer already has an active (WAITING/CALLED/IN_SERVICE) entry.
     */
    public boolean isCustomerAlreadyWaiting(ServiceQueue queue, Customer customer) {
        return entryRepository.existsByQueueAndCustomerAndStatusIn(
            queue, customer,
            Arrays.asList(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.IN_SERVICE));
    }
}
