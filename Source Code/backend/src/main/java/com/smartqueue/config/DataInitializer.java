package com.smartqueue.config;

import com.smartqueue.entity.*;
import com.smartqueue.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner seedDatabase(UserRepository userRepo,
                                   ServiceRepository serviceRepo,
                                   StaffRepository staffRepo,
                                   PasswordEncoder encoder) {
        return args -> {
            // ---- Admin ----
            if (!userRepo.existsByEmail("admin@smartqueue.com")) {
                userRepo.save(new Admin("Alice Admin",
                    "admin@smartqueue.com", encoder.encode("admin123")));
            }

            // ---- Services (save first so we can assign them to staff) ----
            ServiceEntity consultation, labTest, dental, vaccination;
            if (serviceRepo.count() == 0) {
                consultation = serviceRepo.save(new ServiceEntity(
                    "General Consultation",
                    "General medical consultation with a licensed physician", 30));
                labTest = serviceRepo.save(new ServiceEntity(
                    "Lab Test",
                    "Blood, urine, and other diagnostic tests", 15));
                dental = serviceRepo.save(new ServiceEntity(
                    "Dental Checkup",
                    "Routine dental examination and cleaning", 45));
                vaccination = serviceRepo.save(new ServiceEntity(
                    "Vaccination",
                    "Routine and travel vaccinations", 20));
                log.info("Created 4 demo services");
            } else {
                var all  = serviceRepo.findAll();
                consultation = all.get(0);
                labTest      = all.size() > 1 ? all.get(1) : all.get(0);
                dental       = all.size() > 2 ? all.get(2) : all.get(0);
                vaccination  = all.size() > 3 ? all.get(3) : all.get(0);
            }

            // ---- Staff: set assigned services before saving to avoid lazy-load issues ----
            if (!userRepo.existsByEmail("smith@smartqueue.com")) {
                Staff smith = new Staff("Dr. Smith", "smith@smartqueue.com",
                                        encoder.encode("staff123"));
                smith.getAssignedServices().add(consultation);
                smith.getAssignedServices().add(labTest);
                staffRepo.save(smith);
                log.info("Created staff: smith@smartqueue.com / staff123 (Consultation + Lab Test)");
            }
            if (!userRepo.existsByEmail("jane@smartqueue.com")) {
                Staff jane = new Staff("Dr. Jane", "jane@smartqueue.com",
                                       encoder.encode("staff123"));
                jane.getAssignedServices().add(dental);
                jane.getAssignedServices().add(vaccination);
                staffRepo.save(jane);
                log.info("Created staff: jane@smartqueue.com / staff123 (Dental + Vaccination)");
            }

            // ---- Customers ----
            if (!userRepo.existsByEmail("alice@demo.com")) {
                userRepo.save(new Customer("Alice Customer",
                    "alice@demo.com", encoder.encode("customer123"), "555-1001"));
            }
            if (!userRepo.existsByEmail("bob@demo.com")) {
                userRepo.save(new Customer("Bob Customer",
                    "bob@demo.com", encoder.encode("customer123"), "555-1002"));
            }
            log.info("Customers: alice@demo.com / customer123 | bob@demo.com / customer123");
            log.info("SmartQueue ready — open http://localhost:5173");
        };
    }
}
