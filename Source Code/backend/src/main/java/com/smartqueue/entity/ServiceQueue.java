package com.smartqueue.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the virtual queue for a specific service on a given date.
 * Named ServiceQueue to avoid clashing with java.util.Queue.
 *
 * UML Class Diagram: Queue entity with methods getCurrentQueue,
 * addCustomerToQueue, updateQueuePosition, getNextQueueCustomer, updateQueueOrder.
 * These are implemented in QueueManagementService.
 *
 * Relationship: Service 1 -- 0..* Queue (Queue contains 1..* QueueEntry)
 */
@Entity
@Table(name = "service_queues")
public class ServiceQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String status = "OPEN";  // OPEN | CLOSED

    // One Queue → many QueueEntries (composition)
    @OneToMany(mappedBy = "queue", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("queueNumber ASC")
    private List<QueueEntry> entries = new ArrayList<>();

    public ServiceQueue() {}

    public ServiceQueue(ServiceEntity service, LocalDate date) {
        this.service = service;
        this.date    = date;
        this.status  = "OPEN";
    }

    // ---- Getters & Setters ----
    public Long            getId()      { return id; }
    public ServiceEntity   getService() { return service; }
    public LocalDate       getDate()    { return date; }
    public String          getStatus()  { return status; }
    public List<QueueEntry> getEntries(){ return entries; }

    public void setId(Long id)               { this.id = id; }
    public void setService(ServiceEntity s)  { this.service = s; }
    public void setDate(LocalDate d)         { this.date = d; }
    public void setStatus(String s)          { this.status = s; }
    public void setEntries(List<QueueEntry> e){ this.entries = e; }
}
