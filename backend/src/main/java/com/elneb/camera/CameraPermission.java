package com.elneb.camera;
import com.elneb.campus.Classroom;
import com.elneb.auth.User;
import com.elneb.common.UuidEntitySupport;
import jakarta.persistence.*;
@Entity @Table(name="camera_permissions", uniqueConstraints=@UniqueConstraint(name="uk_camera_permission",columnNames={"classroom_id","user_id"}))
public class CameraPermission extends UuidEntitySupport {
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="classroom_id") private Classroom classroom;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="user_id") private User user;
    @Column(nullable=false) private boolean active=true;
    protected CameraPermission(){}
    public CameraPermission(Classroom classroom,User user){this.classroom=classroom;this.user=user;}
    public boolean isActive(){return active;} public Classroom getClassroom(){return classroom;}
}
