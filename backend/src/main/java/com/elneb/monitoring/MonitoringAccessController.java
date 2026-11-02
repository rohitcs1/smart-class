package com.elneb.monitoring;

import com.elneb.auth.*;
import com.elneb.campus.Classroom;
import com.elneb.campus.ClassroomRepository;
import com.elneb.security.TenantAccessService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringAccessController {
    private final UserRepository users;
    private final JwtService jwt;
    private final ClassroomRepository classrooms;
    private final TenantAccessService access;

    public MonitoringAccessController(UserRepository users, JwtService jwt, ClassroomRepository classrooms, TenantAccessService access) {
        this.users = users; this.jwt = jwt; this.classrooms = classrooms; this.access = access;
    }

    @PostMapping("/access-token")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTION_ADMIN','TEACHER','DEVICE_ADMIN')")
    public TokenResponse token(@RequestParam String scope, @RequestParam(required = false) UUID classroomId,
                               Authentication authentication) {
        if (!scope.equals("monitoring:ws") && !scope.equals("camera:stream")) throw new IllegalArgumentException("Unsupported access scope");
        User user = users.findById(UUID.fromString(authentication.getName())).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (scope.equals("camera:stream")) {
            if (classroomId == null) throw new IllegalArgumentException("classroomId is required");
            Classroom room = classrooms.findById(classroomId).orElseThrow(() -> new IllegalArgumentException("Classroom not found"));
            access.requireClassroom(room, authentication);
        }
        return new TokenResponse(jwt.createScoped(user, scope), 300);
    }

    public record TokenResponse(String token, int expiresInSeconds) {}
}
