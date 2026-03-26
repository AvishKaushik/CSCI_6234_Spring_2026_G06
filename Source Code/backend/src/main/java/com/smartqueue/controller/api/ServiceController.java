package com.smartqueue.controller.api;

import com.smartqueue.entity.ServiceEntity;
import com.smartqueue.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Robustness Diagram: Service Controller
 *
 * Handles UC5 — Manage Services.
 *
 * Diagram flow:
 *   ServiceUI → ServiceController.submitServiceChanges()
 *             → ServiceEntity.createService(serviceData)
 *             → ServiceEntity.updateService(serviceID, serviceData)
 *             → ServiceEntity.deleteService(serviceID)
 *             → ServiceEntity.retrieveServices()
 *             → ServiceUI: confirmation(message) / updatedServiceList(services)
 */
@RestController
public class ServiceController {

    private final AdminService adminService;

    public ServiceController(AdminService adminService) {
        this.adminService = adminService;
    }

    /** Admin dashboard — summary counts for services and staff. */
    @GetMapping("/api/admin/dashboard")
    public ResponseEntity<?> dashboard() {
        return ResponseEntity.ok(Map.of(
            "serviceCount", adminService.retrieveServices().size(),
            "staffCount",   adminService.retrieveStaffList().size()
        ));
    }

    /**
     * Diagram: ServiceController → ServiceEntity.retrieveServices()
     * → ServiceUI: updatedServiceList(services)
     */
    @GetMapping("/api/admin/services")
    public ResponseEntity<?> retrieveServices() {
        return ResponseEntity.ok(
            adminService.retrieveServices().stream().map(s -> Map.of(
                "id",          s.getId(),
                "serviceName", s.getServiceName(),
                "description", s.getDescription() == null ? "" : s.getDescription(),
                "duration",    s.getDuration(),
                "available",   s.isAvailable()
            )).toList()
        );
    }

    /**
     * Diagram: ServiceUI → ServiceController.submitServiceChanges()
     *          → ServiceEntity.createService(serviceData)
     *          → ServiceUI: confirmation(message)
     */
    @PostMapping("/api/admin/services")
    public ResponseEntity<?> createService(@RequestBody Map<String, Object> body) {
        String name  = (String)  body.get("serviceName");
        String desc  = (String)  body.getOrDefault("description", "");
        int duration = (Integer) body.get("duration");
        ServiceEntity svc = adminService.createService(name, desc, duration);
        return ResponseEntity.ok(Map.of(
            "id",          svc.getId(),
            "serviceName", svc.getServiceName(),
            "description", svc.getDescription() == null ? "" : svc.getDescription(),
            "duration",    svc.getDuration(),
            "available",   svc.isAvailable()
        ));
    }

    /**
     * Diagram: ServiceUI → ServiceController.submitServiceChanges()
     *          → ServiceEntity.updateService(serviceID, serviceData)
     *          → ServiceUI: confirmation(message)
     */
    @PutMapping("/api/admin/services/{id}")
    public ResponseEntity<?> updateService(@PathVariable Long id,
                                            @RequestBody Map<String, Object> body) {
        String name       = (String)  body.get("serviceName");
        String desc       = (String)  body.getOrDefault("description", "");
        int duration      = (Integer) body.get("duration");
        boolean available = (Boolean) body.getOrDefault("available", true);
        ServiceEntity svc = adminService.updateService(id, name, desc, duration, available);
        return ResponseEntity.ok(Map.of(
            "id",          svc.getId(),
            "serviceName", svc.getServiceName(),
            "description", svc.getDescription() == null ? "" : svc.getDescription(),
            "duration",    svc.getDuration(),
            "available",   svc.isAvailable()
        ));
    }

    /**
     * Diagram: ServiceUI → ServiceController.submitServiceChanges()
     *          → ServiceEntity.deleteService(serviceID)
     *          → ServiceUI: confirmation(message)
     */
    @DeleteMapping("/api/admin/services/{id}")
    public ResponseEntity<?> deleteService(@PathVariable Long id) {
        boolean deleted = adminService.deleteService(id);
        if (!deleted) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("message", "Service deleted"));
    }

    // -----------------------------------------------------------------------
    //  Diagram entity methods — ServiceEntity
    // -----------------------------------------------------------------------

    /**
     * Diagram: ServiceUI → ServiceController.submitServiceChanges(serviceData)
     * Validates and delegates a service mutation to AdminService.
     * Each CRUD endpoint (createService, updateService, deleteService) is a specific
     * invocation of this flow. The shared logic — parse input, call AdminService, return
     * confirmation — is what the diagram arrow represents.
     */
    private void submitServiceChanges() {
        // Traceability placeholder: concrete logic lives in createService / updateService / deleteService above.
    }
}
