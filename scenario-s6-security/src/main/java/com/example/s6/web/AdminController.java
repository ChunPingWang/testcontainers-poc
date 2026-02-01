package com.example.s6.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Admin REST Controller.
 * Requires ADMIN role for access.
 *
 * Demonstrates:
 * - Role-based access control
 * - Admin-only operations
 * - Audit logging with user context
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final Map<UUID, UserRecord> users = new ConcurrentHashMap<>();
    private final List<AuditLog> auditLogs = new CopyOnWriteArrayList<>();

    /**
     * Lists all users in the system.
     * Requires ADMIN role.
     *
     * @return list of all users
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserRecord>> listUsers() {
        return ResponseEntity.ok(List.copyOf(users.values()));
    }

    /**
     * Creates a new user.
     * Requires ADMIN role.
     *
     * @param request the user creation request
     * @param jwt the admin's JWT
     * @return the created user
     */
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserRecord> createUser(
            @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String adminUsername = jwt.getClaimAsString("preferred_username");

        UserRecord user = new UserRecord(
            UUID.randomUUID(),
            request.username(),
            request.email(),
            request.roles(),
            true,
            Instant.now(),
            adminUsername
        );

        users.put(user.id(), user);

        // Audit log
        auditLogs.add(new AuditLog(
            UUID.randomUUID(),
            "CREATE_USER",
            adminUsername,
            "Created user: " + user.username(),
            Instant.now()
        ));

        return ResponseEntity.status(201).body(user);
    }

    /**
     * Deletes a user.
     * Requires ADMIN role.
     *
     * @param id the user ID
     * @param jwt the admin's JWT
     * @return no content on success
     */
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UserRecord removed = users.remove(id);
        if (removed == null) {
            return ResponseEntity.notFound().build();
        }

        String adminUsername = jwt.getClaimAsString("preferred_username");

        // Audit log
        auditLogs.add(new AuditLog(
            UUID.randomUUID(),
            "DELETE_USER",
            adminUsername,
            "Deleted user: " + removed.username(),
            Instant.now()
        ));

        return ResponseEntity.noContent().build();
    }

    /**
     * Gets audit logs.
     * Requires ADMIN role.
     *
     * @return list of audit logs
     */
    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(List.copyOf(auditLogs));
    }

    /**
     * Gets system statistics.
     * Requires ADMIN role.
     *
     * @return system statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemStats> getStats() {
        SystemStats stats = new SystemStats(
            users.size(),
            auditLogs.size(),
            Runtime.getRuntime().freeMemory(),
            Runtime.getRuntime().totalMemory(),
            Instant.now()
        );
        return ResponseEntity.ok(stats);
    }

    /**
     * User record.
     */
    public record UserRecord(
        UUID id,
        String username,
        String email,
        List<String> roles,
        boolean enabled,
        Instant createdAt,
        String createdBy
    ) {}

    /**
     * Create user request.
     */
    public record CreateUserRequest(
        String username,
        String email,
        List<String> roles
    ) {}

    /**
     * Audit log entry.
     */
    public record AuditLog(
        UUID id,
        String action,
        String performedBy,
        String description,
        Instant timestamp
    ) {}

    /**
     * System statistics.
     */
    public record SystemStats(
        int totalUsers,
        int totalAuditLogs,
        long freeMemory,
        long totalMemory,
        Instant timestamp
    ) {}
}
