package com.smartqueue.entity;

import com.smartqueue.enums.UserRole;
import jakarta.persistence.*;

/**
 * Abstract base entity for all users.
 * Maps to a single "users" table (SINGLE_TABLE strategy) with a "role" discriminator column.
 * Mirrors the User abstract class from the UML Class Diagram.
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING)
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;   // BCrypt-encoded

    @Column(name = "role", insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    protected User() {}

    protected User(String name, String email, String password) {
        this.name     = name;
        this.email    = email;
        this.password = password;
    }

    /**
     * Validates credentials against stored values.
     * In Spring Security this is handled by PasswordEncoder.matches();
     * this method is preserved for compatibility with the UML Class Diagram.
     * Diagram: ValidateUser(email : String, password : String) : Boolean
     */
    public boolean validateUser(String email, String password) {
        return this.email.equals(email) && this.password.equals(password);
    }

    public abstract String getRole();

    // ---- Getters & Setters ----
    public Long   getId()       { return id; }
    public String getName()     { return name; }
    public String getEmail()    { return email; }
    public String getPassword() { return password; }

    public void setId(Long id)           { this.id = id; }
    public void setName(String name)     { this.name = name; }
    public void setEmail(String email)   { this.email = email; }
    public void setPassword(String p)    { this.password = p; }

    @Override
    public String toString() {
        return "[" + getRole() + "] " + name + " (" + email + ")";
    }
}
