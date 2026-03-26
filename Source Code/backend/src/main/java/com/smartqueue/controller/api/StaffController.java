package com.smartqueue.controller.api;

import com.smartqueue.dto.StaffForm;
import com.smartqueue.entity.Staff;
import com.smartqueue.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Robustness Diagram: Staff Controller
 *
 * Handles UC6 — Manage Staff Accounts.
 *
 * Diagram flow:
 *   StaffUI → StaffController.submitStaffChanges()
 *           → StaffEntity.createStaff(staffData)
 *           → StaffEntity.updateStaff(staffID, staffData)
 *           → StaffEntity.deleteStaff(staffID)
 *           → StaffEntity.retrieveStaffList()
 *           → StaffUI: confirmation(message) / updatedStaffList(staffList)
 */
@RestController
public class StaffController {

    private final AdminService adminService;

    public StaffController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Diagram: StaffController → StaffEntity.retrieveStaffList()
     * → StaffUI: updatedStaffList(staffList)
     */
    @Transactional(readOnly = true)
    @GetMapping("/api/admin/staff")
    public ResponseEntity<?> retrieveStaffList() {
        return ResponseEntity.ok(
            adminService.retrieveStaffList().stream().map(s -> Map.of(
                "id",                 s.getId(),
                "name",               s.getName(),
                "email",              s.getEmail(),
                "assignedServiceIds", s.getAssignedServices().stream()
                                       .map(svc -> svc.getId()).toList()
            )).toList()
        );
    }

    /**
     * Diagram: StaffUI → StaffController.submitStaffChanges()
     *          → StaffEntity.createStaff(staffData)
     *          → StaffUI: confirmation(message)
     */
    @PostMapping("/api/admin/staff")
    public ResponseEntity<?> createStaff(@RequestBody Map<String, String> body) {
        StaffForm form = new StaffForm();
        form.setName(body.get("name"));
        form.setEmail(body.get("email"));
        form.setPassword(body.get("password"));
        Staff staff = adminService.createStaff(form);
        if (staff == null) return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        return ResponseEntity.ok(Map.of(
            "id",                 staff.getId(),
            "name",               staff.getName(),
            "email",              staff.getEmail(),
            "assignedServiceIds", List.of()
        ));
    }

    /**
     * Diagram: StaffUI → StaffController.submitStaffChanges()
     *          → StaffEntity.updateStaff(staffID, staffData)
     *          → StaffUI: confirmation(message)
     */
    @PutMapping("/api/admin/staff/{id}")
    public ResponseEntity<?> updateStaff(@PathVariable Long id,
                                          @RequestBody Map<String, String> body) {
        StaffForm form = new StaffForm();
        form.setName(body.get("name"));
        form.setEmail(body.get("email"));
        form.setPassword(body.getOrDefault("password", ""));
        Staff staff = adminService.updateStaff(id, form);
        return ResponseEntity.ok(Map.of(
            "id",    staff.getId(),
            "name",  staff.getName(),
            "email", staff.getEmail()
        ));
    }

    /**
     * Diagram: StaffUI → StaffController.submitStaffChanges()
     *          → StaffEntity.deleteStaff(staffID)
     *          → StaffUI: confirmation(message)
     */
    @DeleteMapping("/api/admin/staff/{id}")
    public ResponseEntity<?> deleteStaff(@PathVariable Long id) {
        boolean deleted = adminService.deleteStaff(id);
        if (!deleted) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("message", "Staff deleted"));
    }

    // ---- Service assignment (admin assigns services to staff) ----

    @Transactional(readOnly = true)
    @GetMapping("/api/admin/staff/{id}/services")
    public ResponseEntity<?> getStaffServices(@PathVariable Long id) {
        Staff staff = adminService.findStaff(id).orElse(null);
        if (staff == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(
            staff.getAssignedServices().stream()
                .map(s -> Map.of("id", s.getId(), "serviceName", s.getServiceName()))
                .toList()
        );
    }

    @Transactional
    @PutMapping("/api/admin/staff/{id}/services")
    public ResponseEntity<?> assignServices(@PathVariable Long id,
                                             @RequestBody List<Long> serviceIds) {
        adminService.assignServices(id, serviceIds);
        return ResponseEntity.ok(Map.of("message", "Services assigned"));
    }

    // -----------------------------------------------------------------------
    //  Diagram entity methods — StaffEntity
    // -----------------------------------------------------------------------

    /**
     * Diagram: StaffUI → StaffController.submitStaffChanges(staffData)
     * Validates and delegates a staff mutation to AdminService.
     * Each CRUD endpoint (createStaff, updateStaff, deleteStaff) is a specific
     * invocation of this flow. The shared logic — parse input, call AdminService, return
     * confirmation — is what the diagram arrow represents.
     */
    private void submitStaffChanges() {
        // Traceability placeholder: concrete logic lives in createStaff / updateStaff / deleteStaff above.
    }
}
