package com.elneb.campus;

import com.elneb.common.UuidEntitySupport;
import com.elneb.institution.Institution;
import jakarta.persistence.*;

@Entity
@Table(name = "campuses")
public class Campus extends UuidEntitySupport {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(length = 500)
    private String address;
    @Column(nullable = false)
    private boolean active = true;

    protected Campus() {}
    public Campus(Institution institution, String name, String address) {
        this.institution = institution;
        this.name = name.trim();
        this.address = address;
    }
    public Institution getInstitution() { return institution; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public boolean isActive() { return active; }
    public void update(String name, String address) { this.name = name.trim(); this.address = address; }
    public void setActive(boolean active) { this.active = active; }
}
