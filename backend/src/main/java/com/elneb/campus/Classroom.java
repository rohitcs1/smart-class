package com.elneb.campus;

import com.elneb.common.UuidEntitySupport;
import jakarta.persistence.*;

@Entity
@Table(name = "classrooms")
public class Classroom extends UuidEntitySupport {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false)
    private int capacity;
    @Column(name = "room_type", nullable = false, length = 50)
    private String roomType;
    @Column(nullable = false)
    private boolean active = true;
    protected Classroom() {}
    public Classroom(Floor floor, String name, int capacity, String roomType) {
        this.floor = floor; this.name = name.trim(); this.capacity = capacity; this.roomType = roomType.trim();
    }
    public Floor getFloor() { return floor; }
    public String getName() { return name; }
    public int getCapacity() { return capacity; }
    public String getRoomType() { return roomType; }
    public boolean isActive() { return active; }
}
