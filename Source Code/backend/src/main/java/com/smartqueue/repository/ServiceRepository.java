package com.smartqueue.repository;

import com.smartqueue.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
    List<ServiceEntity> findByIsAvailableTrue();
}
