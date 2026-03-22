package com.smartqueue.entity;

import com.smartqueue.enums.QueueStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One customer's session within a ServiceQueue.
 *
 * Tracks queueNumber (unique, never changes), position (recalculated),
 * joinedAt timestamp, QueueStatus, and VIP flag.
 *
 * State transitions (from UML State Diagram):
 *   WAITING → CALLED → IN_SERVICE → COMPLETED
 *   WAITING → LEFT | NO_SHOW
 *   CALLED  → NO_SHOW
 */
@Entity
@Table(name = "queue_entries")
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_id", nullable = false)
    private ServiceQueue queue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "queue_number", nullable = false)
    private int queueNumber;

    @Column(nullable = false)
    private int position;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueStatus status = QueueStatus.WAITING;

    @Column(name = "is_vip", nullable = false)
    private boolean isVIP = false;

    public QueueEntry() {}

    public QueueEntry(ServiceQueue queue, Customer customer, int queueNumber, int position) {
        this.queue       = queue;
        this.customer    = customer;
        this.queueNumber = queueNumber;
        this.position    = position;
        this.joinedAt    = LocalDateTime.now();
        this.status      = QueueStatus.WAITING;
    }

    // ---- State transition methods (preserved from UML) ----

    /** WAITING → CALLED */
    public void callCustomer() {
        if (status == QueueStatus.WAITING) status = QueueStatus.CALLED;
    }

    /** CALLED → IN_SERVICE */
    public void startService() {
        if (status == QueueStatus.CALLED) status = QueueStatus.IN_SERVICE;
    }

    /** IN_SERVICE → COMPLETED */
    public void completeService() {
        if (status == QueueStatus.IN_SERVICE) status = QueueStatus.COMPLETED;
    }

    /** WAITING | CALLED → NO_SHOW */
    public void markNoShow() {
        if (status == QueueStatus.WAITING || status == QueueStatus.CALLED)
            status = QueueStatus.NO_SHOW;
    }

    /** WAITING → LEFT */
    public void markLeft() {
        if (status == QueueStatus.WAITING) status = QueueStatus.LEFT;
    }

    // ---- Getters & Setters ----
    public Long          getId()          { return id; }
    public ServiceQueue  getQueue()       { return queue; }
    public Customer      getCustomer()    { return customer; }
    public int           getQueueNumber() { return queueNumber; }
    public int           getPosition()    { return position; }
    public LocalDateTime getJoinedAt()    { return joinedAt; }
    public QueueStatus   getStatus()      { return status; }
    public boolean       isVIP()          { return isVIP; }

    public void setId(Long id)               { this.id = id; }
    public void setQueue(ServiceQueue q)     { this.queue = q; }
    public void setCustomer(Customer c)      { this.customer = c; }
    public void setQueueNumber(int n)        { this.queueNumber = n; }
    public void setPosition(int p)           { this.position = p; }
    public void setJoinedAt(LocalDateTime t) { this.joinedAt = t; }
    public void setStatus(QueueStatus s)     { this.status = s; }
    public void setVIP(boolean v)            { this.isVIP = v; }
}
