package com.elneb.teacher;

import com.elneb.common.UuidEntitySupport;
import com.elneb.institution.Institution;
import jakarta.persistence.*;

@Entity
@Table(name = "teachers")
public class Teacher extends UuidEntitySupport {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;
    @Column(name = "employee_code", nullable = false, length = 80)
    private String employeeCode;
    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;
    @Column(length = 255)
    private String email;
    @Column(length = 150)
    private String department;
    @Column(nullable = false)
    private boolean active = true;
    protected Teacher() {}
    public Teacher(Institution institution, String employeeCode, String fullName, String email, String department) {
        this.institution = institution; this.employeeCode = employeeCode.trim().toUpperCase();
        this.fullName = fullName.trim(); this.email = email; this.department = department;
    }
    public String getEmployeeCode() { return employeeCode; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getDepartment() { return department; }
    public boolean isActive() { return active; }
    public Institution getInstitution() { return institution; }
    public void update(String employeeCode, String fullName, String email, String department) {
        this.employeeCode = employeeCode.trim().toUpperCase(); this.fullName = fullName.trim();
        this.email = email; this.department = department;
    }
    public void setActive(boolean active) { this.active = active; }
}
