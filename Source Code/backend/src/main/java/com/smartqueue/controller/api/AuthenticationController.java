package com.smartqueue.controller.api;

import com.smartqueue.dto.RegistrationForm;
import com.smartqueue.entity.User;
import com.smartqueue.repository.UserRepository;
import com.smartqueue.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Robustness Diagram: Authentication Controller
 *
 * Handles UC1 — Register / Login and Handle Invalid Credentials.
 *
 * Diagram flows:
 *   RegisterOrLogin:        LoginUI → AuthenticationController.submitCredentials()
 *                                   → UserEntity.validateUser()
 *                                   → LoginUI: success / error message
 *
 *   HandleInvalidCredentials: LoginUI → AuthenticationController.submitCredentials()
 *                                      → UserEntity.validateUser()
 *                                      → LoginUI: displayError("Invalid credentials")
 */
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final UserService            userService;
    private final UserRepository         userRepository;
    private final AuthenticationManager  authenticationManager;

    private final HttpSessionSecurityContextRepository contextRepository =
        new HttpSessionSecurityContextRepository();

    public AuthenticationController(UserService userService,
                                     UserRepository userRepository,
                                     AuthenticationManager authenticationManager) {
        this.userService           = userService;
        this.userRepository        = userRepository;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Diagram: LoginUI → AuthenticationController.submitCredentials(email, password)
     *          → UserEntity.validateUser(email, password)
     *          → LoginUI: success(userRole) / displayError("Invalid credentials")
     *
     * Handles both RegisterOrLogin (success path) and HandleInvalidCredentials (error path).
     */
    @PostMapping("/login")
    public ResponseEntity<?> submitCredentials(@RequestBody Map<String, String> body,
                                                HttpServletRequest request,
                                                HttpServletResponse response) {
        String email    = body.get("email");
        String password = body.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password required"));
        }

        try {
            // UserEntity.validateUser(email, password)
            Authentication authenticated = validateUser(email, password);

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authenticated);
            SecurityContextHolder.setContext(context);
            contextRepository.saveContext(context, request, response);

            String role = authenticated.getAuthorities().iterator().next().getAuthority();
            return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "role",          role,
                "email",         email
            ));
        } catch (AuthenticationException e) {
            // displayError("Invalid credentials") path from HandleInvalidCredentials diagram
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
    }

    // -----------------------------------------------------------------------
    //  Diagram entity methods — UserEntity
    // -----------------------------------------------------------------------

    /**
     * Diagram: UserEntity.validateUser(email, password)
     * Authenticates the user via Spring Security AuthenticationManager.
     * Returns the authenticated token on success; throws AuthenticationException on failure.
     */
    private Authentication validateUser(String email, String password) {
        Authentication token = new UsernamePasswordAuthenticationToken(email, password);
        return authenticationManager.authenticate(token);
    }

    /** Invalidates the session and clears the security context. */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    /** Returns current authenticated user info — used by React on page load. */
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }
        return ResponseEntity.ok(Map.of(
            "id",    user.getId(),
            "name",  user.getName(),
            "email", user.getEmail(),
            "role",  user.getRole().toString()
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationForm form) {
        if (form.getPassword() == null || !form.getPassword().equals(form.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Passwords do not match"));
        }
        boolean ok = userService.registerCustomer(form);
        if (!ok) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }
        return ResponseEntity.ok(Map.of("message", "Registration successful"));
    }
}
