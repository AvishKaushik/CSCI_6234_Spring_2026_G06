package com.smartqueue.service;

import com.smartqueue.dto.StaffForm;
import com.smartqueue.entity.ServiceEntity;
import com.smartqueue.entity.Staff;
import com.smartqueue.repository.ServiceRepository;
import com.smartqueue.repository.StaffRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Handles UC5 (Manage Services) and UC6 (Manage Staff Accounts).
 *
 * Method names preserved from UML Class and Sequence Diagrams:
 *   createService, updateService, deleteService, retrieveServices
 *   createStaff,   updateStaff,   deleteStaff,   retrieveStaffList
 */
@Service
public class AdminService {

    private final ServiceRepository serviceRepository;
    private final StaffRepository   staffRepository;
    private final PasswordEncoder   passwordEncoder;

    public AdminService(ServiceRepository serviceRepository,
                        StaffRepository   staffRepository,
                        PasswordEncoder   passwordEncoder) {
        this.serviceRepository = serviceRepository;
        this.staffRepository   = staffRepository;
        this.passwordEncoder   = passwordEncoder;
    }

    // -----------------------------------------------------------------------
    //  UC5 — Manage Services (CRUD)
    // -----------------------------------------------------------------------

    /**
     * Creates a new service.
     * Diagram: ServiceController → ServiceEntity.createService(serviceData)
     */
    @Transactional
    public ServiceEntity createService(String name, String description, int duration) {
        ServiceEntity service = new ServiceEntity(name, description, duration);
        return serviceRepository.save(service);
    }

    /**
     * Updates an existing service's details.
     * Diagram: ServiceController → ServiceEntity.updateService(serviceID, serviceData)
     */
    @Transactional
    public ServiceEntity updateService(Long id, String name, String description,
                                        int duration, boolean isAvailable) {
        ServiceEntity service = serviceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));
        service.setServiceName(name);
        service.setDescription(description);
        service.setDuration(duration);
        service.setAvailable(isAvailable);
        return serviceRepository.save(service);
    }

    /**
     * Removes a service.
     * Diagram: ServiceController → ServiceEntity.deleteService(serviceID) : Boolean
     */
    @Transactional
    public boolean deleteService(Long id) {
        if (!serviceRepository.existsById(id)) return false;
        serviceRepository.deleteById(id);
        return true;
    }

    /**
     * Returns all services.
     * Diagram: ServiceController → ServiceEntity.retrieveServices() : List<Service>
     */
    public List<ServiceEntity> retrieveServices() {
        return serviceRepository.findAll();
    }

    public Optional<ServiceEntity> findService(Long id) {
        return serviceRepository.findById(id);
    }

    public List<ServiceEntity> getAvailableServices() {
        return serviceRepository.findByIsAvailableTrue();
    }

    // -----------------------------------------------------------------------
    //  UC6 — Manage Staff Accounts (CRUD)
    // -----------------------------------------------------------------------

    /**
     * Creates a new staff account.
     * Diagram: StaffController → StaffEntity.createStaff(staffData) : Staff
     *
     * @return the created Staff, or null if email is already taken
     */
    @Transactional
    public Staff createStaff(StaffForm form) {
        if (staffRepository.findByEmail(form.getEmail()).isPresent()) return null;
        Staff staff = new Staff(
            form.getName(),
            form.getEmail(),
            passwordEncoder.encode(form.getPassword())
        );
        return staffRepository.save(staff);
    }

    /**
     * Updates a staff member's details.
     * Diagram: StaffController → StaffEntity.updateStaff(staffID, staffData) : Staff
     */
    @Transactional
    public Staff updateStaff(Long id, StaffForm form) {
        Staff staff = staffRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + id));
        staff.setName(form.getName());
        staff.setEmail(form.getEmail());
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            staff.setPassword(passwordEncoder.encode(form.getPassword()));
        }
        return staffRepository.save(staff);
    }

    /**
     * Removes a staff account.
     * Diagram: StaffController → StaffEntity.deleteStaff(staffID) : Boolean
     */
    @Transactional
    public boolean deleteStaff(Long id) {
        if (!staffRepository.existsById(id)) return false;
        staffRepository.deleteById(id);
        return true;
    }

    /**
     * Returns all registered staff.
     * Diagram: StaffController → StaffEntity.retrieveStaffList() : List<Staff>
     */
    public List<Staff> retrieveStaffList() {
        return staffRepository.findAll();
    }

    public Optional<Staff> findStaff(Long id) {
        return staffRepository.findById(id);
    }

    /** Fix 2: Assign a set of services to a staff member (replaces existing assignment). */
    @Transactional
    public void assignServices(Long staffId, List<Long> serviceIds) {
        Staff staff = staffRepository.findById(staffId)
            .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));
        Set<ServiceEntity> services = new java.util.HashSet<>(serviceRepository.findAllById(serviceIds));
        staff.setAssignedServices(services);
        staffRepository.save(staff);
    }
}
