package com.smartqueue.repository;

import com.smartqueue.entity.Customer;
import com.smartqueue.entity.QueueEntry;
import com.smartqueue.entity.ServiceQueue;
import com.smartqueue.enums.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    List<QueueEntry> findByQueueAndStatusOrderByPositionAsc(ServiceQueue queue, QueueStatus status);

    List<QueueEntry> findByQueueOrderByQueueNumberAsc(ServiceQueue queue);

    Optional<QueueEntry> findFirstByQueueAndStatusOrderByPositionAsc(ServiceQueue queue, QueueStatus status);

    Optional<QueueEntry> findByQueueAndCustomerAndStatus(ServiceQueue queue, Customer customer, QueueStatus status);

    List<QueueEntry> findByCustomerOrderByJoinedAtDesc(Customer customer);

    @Query("SELECT COUNT(e) FROM QueueEntry e WHERE e.queue = :queue AND e.status = 'WAITING'")
    int countWaiting(ServiceQueue queue);

    boolean existsByQueueAndCustomerAndStatusIn(ServiceQueue queue, Customer customer, List<QueueStatus> statuses);

    boolean existsByQueueAndStatusIn(ServiceQueue queue, List<QueueStatus> statuses);
}
