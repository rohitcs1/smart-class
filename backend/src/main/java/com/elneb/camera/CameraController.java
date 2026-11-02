package com.elneb.camera;

import com.elneb.auth.*;
import com.elneb.campus.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.io.*;
import java.net.*;
import java.util.*;
import com.elneb.security.TenantAccessService;
import org.springframework.security.core.Authentication;

@RestController @RequestMapping("/api/cameras")
public class CameraController {
    private final ClassroomRepository classrooms; private final UserRepository users; private final CameraPermissionRepository permissions; private final JwtService jwt;
    private final TenantAccessService access;
    public CameraController(ClassroomRepository classrooms,UserRepository users,CameraPermissionRepository permissions, TenantAccessService access, JwtService jwt){this.classrooms=classrooms;this.users=users;this.permissions=permissions;this.access=access;this.jwt=jwt;}
    @PostMapping("/{classroomId}/permission")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTION_ADMIN')")
    public PermissionResponse grant(@PathVariable UUID classroomId,@Valid @RequestBody PermissionRequest request, Authentication authentication){
        Classroom room=classrooms.findById(classroomId).orElseThrow(()->new IllegalArgumentException("Classroom not found"));
        access.requireClassroom(room, authentication);
        User user=users.findById(request.userId()).orElseThrow(()->new IllegalArgumentException("User not found"));
        if (!access.isSuperAdmin(authentication) && (user.getTenant() == null || !user.getTenant().getId().equals(access.current(authentication).getTenant().getId()))) throw new org.springframework.security.access.AccessDeniedException("User is outside your tenant");
        CameraPermission permission=permissions.findByClassroomIdAndUserId(classroomId,user.getId()).orElseGet(()->new CameraPermission(room,user));
        return new PermissionResponse(permissions.save(permission).getId(),classroomId,user.getId());
    }
    @PutMapping("/{classroomId}/source")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTION_ADMIN')")
    public void setSource(@PathVariable UUID classroomId,@Valid @RequestBody SourceRequest request, Authentication authentication){
        Classroom room=classrooms.findById(classroomId).orElseThrow(()->new IllegalArgumentException("Classroom not found"));
        access.requireClassroom(room, authentication);
        validateCameraUrl(request.cameraUrl());
        room.setCameraUrl(request.cameraUrl()); classrooms.save(room);
    }

    @GetMapping("/{classroomId}/permissions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTION_ADMIN')")
    public List<PermissionResponse> listPermissions(@PathVariable UUID classroomId, Authentication authentication) {
        Classroom room = classrooms.findById(classroomId).orElseThrow(() -> new IllegalArgumentException("Classroom not found"));
        access.requireClassroom(room, authentication);
        return permissions.findAll().stream().filter(item -> item.getClassroom().getId().equals(classroomId))
                .map(item -> new PermissionResponse(item.getId(), classroomId, item.getUser().getId(), item.isActive())).toList();
    }

    @DeleteMapping("/{classroomId}/permission/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTION_ADMIN')")
    public void revoke(@PathVariable UUID classroomId, @PathVariable UUID userId, Authentication authentication) {
        Classroom room = classrooms.findById(classroomId).orElseThrow(() -> new IllegalArgumentException("Classroom not found"));
        access.requireClassroom(room, authentication);
        permissions.findByClassroomIdAndUserId(classroomId, userId).ifPresent(item -> { item.setActive(false); permissions.save(item); });
    }
    @GetMapping(value="/{classroomId}/stream",produces="multipart/x-mixed-replace; boundary=frame")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StreamingResponseBody> stream(@PathVariable UUID classroomId, @RequestParam String token, Authentication authentication){
        if (!"camera:stream".equals(jwt.parse(token).get("scope", String.class))) throw new org.springframework.security.access.AccessDeniedException("Short-lived camera token required");
        Classroom room=classrooms.findById(classroomId).orElseThrow(()->new IllegalArgumentException("Classroom not found"));
        access.requireClassroom(room, authentication);
        UUID userId=UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        User user=users.findById(userId).orElseThrow(()->new IllegalArgumentException("User not found"));
        boolean admin=user.getRole()==Role.SUPER_ADMIN||user.getRole()==Role.INSTITUTION_ADMIN;
        if(!admin&&!permissions.findByClassroomIdAndUserId(classroomId,userId).map(CameraPermission::isActive).orElse(false))throw new org.springframework.security.access.AccessDeniedException("Camera access is not granted");
        if(room.getCameraUrl()==null)throw new IllegalArgumentException("Camera source is not configured");
        StreamingResponseBody body=output->{validateCameraUrl(room.getCameraUrl()); HttpURLConnection connection=(HttpURLConnection)new URL(room.getCameraUrl()).openConnection(); connection.setInstanceFollowRedirects(false); connection.setConnectTimeout(5000); connection.setReadTimeout(0); int status=connection.getResponseCode(); if(status<200||status>=300) throw new IOException("Camera source returned HTTP "+status); try(InputStream input=connection.getInputStream()){input.transferTo(output);}finally{connection.disconnect();}};
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
    public record PermissionRequest(@NotNull UUID userId){} public record SourceRequest(@jakarta.validation.constraints.NotBlank String cameraUrl){} public record PermissionResponse(UUID id,UUID classroomId,UUID userId,boolean active){ public PermissionResponse(UUID id,UUID classroomId,UUID userId){this(id,classroomId,userId,true);} }

    private static void validateCameraUrl(String value) {
        try {
            URI uri = URI.create(value);
            if (!List.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null) throw new IllegalArgumentException();
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() ||
                        address.isSiteLocalAddress() || address.getHostAddress().equals("0.0.0.0") ||
                        address.getHostAddress().equals("::")) throw new IllegalArgumentException();
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Camera URL must target a public HTTP(S) host");
        }
    }
}
