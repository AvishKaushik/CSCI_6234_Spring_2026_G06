package com.smartqueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SmartQueue – Appointment & Queue Management System
 *
 * Spring Boot entry point. Run this class to start the embedded Tomcat server
 * on http://localhost:8080
 *
 * Demo accounts (auto-seeded on startup):
 *   Admin:    admin@smartqueue.com / admin123
 *   Staff:    smith@smartqueue.com / staff123
 *   Customer: alice@demo.com       / customer123
 */
@SpringBootApplication
public class SmartQueueApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartQueueApplication.class, args);
    }
}
