package com.elneb.campus;

import com.elneb.common.UuidEntitySupport;
import jakarta.persistence.*;

@Entity
@Table(name = "buildings")
public class Building extends UuidEntitySupport {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id", nullable = false)
    private Campus campus;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(nullable = false)
    private boolean active = true;
    protected Building() {}
    public Building(Campus campus, String name) { this.campus = campus; this.name = name.trim(); }
    public Campus getCampus() { return campus; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public void update(String name) { this.name = name.trim(); }
    public void setActive(boolean active) { this.active = active; }
}
