package com.smartqueue.repository;

import com.smartqueue.entity.ServiceEntity;
import com.smartqueue.entity.ServiceQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface ServiceQueueRepository extends JpaRepository<ServiceQueue, Long> {

    Optional<ServiceQueue> findByServiceAndDate(ServiceEntity service, LocalDate date);

    boolean existsByServiceAndDate(ServiceEntity service, LocalDate date);
}
