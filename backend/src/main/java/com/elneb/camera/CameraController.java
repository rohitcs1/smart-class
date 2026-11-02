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

@RestController @RequestMapping("/api/cameras")
public class CameraController {
    private final ClassroomRepository classrooms; private final UserRepository users; private final CameraPermissionRepository permissions;
    public CameraController(ClassroomRepository classrooms,UserRepository users,CameraPermissionRepository permissions){this.classrooms=classrooms;this.users=users;this.permissions=permissions;}
    @PostMapping("/{classroomId}/permission")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTION_ADMIN')")
    public PermissionResponse grant(@PathVariable UUID classroomId,@Valid @RequestBody PermissionRequest request){
        Classroom room=classrooms.findById(classroomId).orElseThrow(()->new IllegalArgumentException("Classroom not found"));
        User user=users.findById(request.userId()).orElseThrow(()->new IllegalArgumentException("User not found"));
        CameraPermission permission=permissions.findByClassroomIdAndUserId(classroomId,user.getId()).orElseGet(()->new CameraPermission(room,user));
        return new PermissionResponse(permissions.save(permission).getId(),classroomId,user.getId());
    }
    @PutMapping("/{classroomId}/source")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTION_ADMIN')")
    public void setSource(@PathVariable UUID classroomId,@Valid @RequestBody SourceRequest request){
        Classroom room=classrooms.findById(classroomId).orElseThrow(()->new IllegalArgumentException("Classroom not found"));
        try{URI uri=URI.create(request.cameraUrl()); if(!List.of("http","https").contains(uri.getScheme())||uri.getHost()==null)throw new IllegalArgumentException("Camera URL must be a valid HTTP(S) URL");}catch(IllegalArgumentException ex){throw new IllegalArgumentException("Camera URL must be a valid HTTP(S) URL");}
        room.setCameraUrl(request.cameraUrl()); classrooms.save(room);
    }
    @GetMapping(value="/{classroomId}/stream",produces="multipart/x-mixed-replace; boundary=frame")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StreamingResponseBody> stream(@PathVariable UUID classroomId){
        Classroom room=classrooms.findById(classroomId).orElseThrow(()->new IllegalArgumentException("Classroom not found"));
        UUID userId=UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        User user=users.findById(userId).orElseThrow(()->new IllegalArgumentException("User not found"));
        boolean admin=user.getRole()==Role.SUPER_ADMIN||user.getRole()==Role.INSTITUTION_ADMIN;
        if(!admin&&!permissions.findByClassroomIdAndUserId(classroomId,userId).map(CameraPermission::isActive).orElse(false))throw new org.springframework.security.access.AccessDeniedException("Camera access is not granted");
        if(room.getCameraUrl()==null)throw new IllegalArgumentException("Camera source is not configured");
        StreamingResponseBody body=output->{HttpURLConnection connection=(HttpURLConnection)new URL(room.getCameraUrl()).openConnection();connection.setConnectTimeout(5000);connection.setReadTimeout(0);try(InputStream input=connection.getInputStream()){input.transferTo(output);}finally{connection.disconnect();}};
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
    public record PermissionRequest(@NotNull UUID userId){} public record SourceRequest(String cameraUrl){} public record PermissionResponse(UUID id,UUID classroomId,UUID userId){}
}
