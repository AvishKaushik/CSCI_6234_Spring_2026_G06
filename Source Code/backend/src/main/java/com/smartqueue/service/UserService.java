package com.smartqueue.service;

import com.smartqueue.dto.RegistrationForm;
import com.smartqueue.entity.Customer;
import com.smartqueue.entity.User;
import com.smartqueue.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implements Spring Security's UserDetailsService so Spring can load users
 * from the database during authentication.
 *
 * Also handles customer self-registration.
 *
 * The validateUser() concept from the UML is implemented via
 * Spring Security's PasswordEncoder.matches() inside loadUserByUsername().
 */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Spring Security calls this during login.
     * Loads the user by email and wraps them in a UserDetails object.
     * The role is mapped to a Spring GrantedAuthority (ROLE_CUSTOMER, etc.).
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }

    /**
     * Registers a new Customer account from the self-registration form.
     * Passwords are BCrypt-encoded before storage.
     *
     * @return true on success, false if email is already taken
     */
    @Transactional
    public boolean registerCustomer(RegistrationForm form) {
        if (userRepository.existsByEmail(form.getEmail())) {
            return false;
        }
        Customer customer = new Customer(
            form.getName(),
            form.getEmail(),
            passwordEncoder.encode(form.getPassword()),
            form.getPhoneNumber()
        );
        userRepository.save(customer);
        return true;
    }

    /**
     * Looks up any user by email (admin utility).
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
}
