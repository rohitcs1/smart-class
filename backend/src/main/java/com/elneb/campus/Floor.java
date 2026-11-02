package com.elneb.campus;

import com.elneb.common.UuidEntitySupport;
import jakarta.persistence.*;

@Entity
@Table(name = "floors")
public class Floor extends UuidEntitySupport {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false)
    private boolean active = true;
    protected Floor() {}
    public Floor(Building building, String name) { this.building = building; this.name = name.trim(); }
    public Building getBuilding() { return building; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public void update(String name) { this.name = name.trim(); }
    public void setActive(boolean active) { this.active = active; }
}
