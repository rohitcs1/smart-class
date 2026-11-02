package com.elneb.security;

import com.elneb.auth.*;
import com.elneb.campus.*;
import com.elneb.institution.Institution;
import com.elneb.institution.InstitutionRepository;
import com.elneb.teacher.Teacher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TenantAccessService {
    private final UserRepository users;
    private final InstitutionRepository institutions;

    public TenantAccessService(UserRepository users, InstitutionRepository institutions) {
        this.users = users; this.institutions = institutions;
    }

    public boolean isSuperAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    public User current(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) throw new AccessDeniedException("Authentication required");
        return users.findById(UUID.fromString(authentication.getName()))
                .orElseThrow(() -> new AccessDeniedException("User not found"));
    }

    public UUID tenantId(Authentication authentication) {
        return current(authentication).getTenant() == null ? null : current(authentication).getTenant().getId();
    }

    public void requireInstitution(UUID institutionId, Authentication authentication) {
        if (isSuperAdmin(authentication)) return;
        User user = current(authentication);
        if (user.getTenant() == null) throw new AccessDeniedException("Tenant access denied");
        Institution institution = institutions.findById(institutionId)
                .orElseThrow(() -> new AccessDeniedException("Institution access denied"));
        if (!user.getTenant().getId().equals(institution.getTenant().getId()))
            throw new AccessDeniedException("Tenant access denied");
    }

    public void requireCampus(Campus campus, Authentication authentication) {
        requireInstitution(campus.getInstitution().getId(), authentication);
    }

    public void requireBuilding(Building building, Authentication authentication) {
        requireCampus(building.getCampus(), authentication);
    }

    public void requireFloor(Floor floor, Authentication authentication) {
        requireBuilding(floor.getBuilding(), authentication);
    }

    public void requireClassroom(Classroom classroom, Authentication authentication) {
        requireFloor(classroom.getFloor(), authentication);
    }

    public void requireTeacher(Teacher teacher, Authentication authentication) {
        requireInstitution(teacher.getInstitution().getId(), authentication);
    }
}
