package com.smartqueue.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form backing object for the customer self-registration page.
 */
public class RegistrationForm {

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Invalid email address")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @Size(min = 6, message = "Password must be at least 6 characters")
    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;

    // ---- Getters & Setters ----
    public String getName()            { return name; }
    public String getEmail()           { return email; }
    public String getPhoneNumber()     { return phoneNumber; }
    public String getPassword()        { return password; }
    public String getConfirmPassword() { return confirmPassword; }

    public void setName(String n)            { this.name = n; }
    public void setEmail(String e)           { this.email = e; }
    public void setPhoneNumber(String p)     { this.phoneNumber = p; }
    public void setPassword(String p)        { this.password = p; }
    public void setConfirmPassword(String p) { this.confirmPassword = p; }

    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }
}
