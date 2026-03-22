package com.smartqueue.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form backing object for Admin creating/updating a Staff account.
 * Mirrors the diagram method: createStaff(staffData), updateStaff(staffID, staffData)
 */
public class StaffForm {

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Invalid email address")
    @NotBlank(message = "Email is required")
    private String email;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;  // optional on update

    // ---- Getters & Setters ----
    public String getName()     { return name; }
    public String getEmail()    { return email; }
    public String getPassword() { return password; }

    public void setName(String n)     { this.name = n; }
    public void setEmail(String e)    { this.email = e; }
    public void setPassword(String p) { this.password = p; }
}
