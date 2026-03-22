package com.smartqueue.entity;

import jakarta.persistence.*;

/**
 * Admin entity — manages services and staff accounts.
 * Discriminator value "ADMIN" in the users table.
 */
@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends User {

    public Admin() {}

    public Admin(String name, String email, String password) {
        super(name, email, password);
    }

    @Override
    public String getRole() { return "ADMIN"; }
}
