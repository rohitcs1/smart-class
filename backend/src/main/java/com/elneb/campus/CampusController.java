package com.elneb.campus;

import com.elneb.institution.Institution;
import com.elneb.institution.InstitutionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/campus")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTION_ADMIN')")
public class CampusController {
    private final CampusRepository campuses;
    private final BuildingRepository buildings;
    private final FloorRepository floors;
    private final ClassroomRepository classrooms;
    private final InstitutionRepository institutions;

    public CampusController(CampusRepository campuses, BuildingRepository buildings, FloorRepository floors,
                            ClassroomRepository classrooms, InstitutionRepository institutions) {
        this.campuses = campuses; this.buildings = buildings; this.floors = floors;
        this.classrooms = classrooms; this.institutions = institutions;
    }

    @GetMapping("/institution/{institutionId}")
    public List<CampusResponse> listCampuses(@PathVariable UUID institutionId) {
        return campuses.findAllByInstitutionIdOrderByName(institutionId).stream().map(CampusResponse::from).toList();
    }

    @PostMapping("/institution/{institutionId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CampusResponse createCampus(@PathVariable UUID institutionId, @Valid @RequestBody CampusRequest request) {
        if (campuses.existsByInstitutionIdAndNameIgnoreCase(institutionId, request.name())) throw new IllegalArgumentException("Campus name is already in use");
        Institution institution = institutions.findById(institutionId).orElseThrow(() -> new IllegalArgumentException("Institution not found"));
        return CampusResponse.from(campuses.save(new Campus(institution, request.name(), request.address())));
    }

    @GetMapping("/{campusId}/buildings")
    public List<BuildingResponse> listBuildings(@PathVariable UUID campusId) {
        return buildings.findAllByCampusIdOrderByName(campusId).stream().map(BuildingResponse::from).toList();
    }

    @PostMapping("/{campusId}/buildings")
    @ResponseStatus(HttpStatus.CREATED)
    public BuildingResponse createBuilding(@PathVariable UUID campusId, @Valid @RequestBody NameRequest request) {
        if (buildings.existsByCampusIdAndNameIgnoreCase(campusId, request.name())) throw new IllegalArgumentException("Building name is already in use");
        Campus campus = campuses.findById(campusId).orElseThrow(() -> new IllegalArgumentException("Campus not found"));
        return BuildingResponse.from(buildings.save(new Building(campus, request.name())));
    }

    @GetMapping("/buildings/{buildingId}/floors")
    public List<FloorResponse> listFloors(@PathVariable UUID buildingId) {
        return floors.findAllByBuildingIdOrderByName(buildingId).stream().map(FloorResponse::from).toList();
    }

    @PostMapping("/buildings/{buildingId}/floors")
    @ResponseStatus(HttpStatus.CREATED)
    public FloorResponse createFloor(@PathVariable UUID buildingId, @Valid @RequestBody NameRequest request) {
        if (floors.existsByBuildingIdAndNameIgnoreCase(buildingId, request.name())) throw new IllegalArgumentException("Floor name is already in use");
        Building building = buildings.findById(buildingId).orElseThrow(() -> new IllegalArgumentException("Building not found"));
        return FloorResponse.from(floors.save(new Floor(building, request.name())));
    }

    @GetMapping("/floors/{floorId}/classrooms")
    public List<ClassroomResponse> listClassrooms(@PathVariable UUID floorId) {
        return classrooms.findAllByFloorIdOrderByName(floorId).stream().map(ClassroomResponse::from).toList();
    }

    @PostMapping("/floors/{floorId}/classrooms")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassroomResponse createClassroom(@PathVariable UUID floorId, @Valid @RequestBody ClassroomRequest request) {
        if (classrooms.existsByFloorIdAndNameIgnoreCase(floorId, request.name())) throw new IllegalArgumentException("Classroom name is already in use");
        Floor floor = floors.findById(floorId).orElseThrow(() -> new IllegalArgumentException("Floor not found"));
        return ClassroomResponse.from(classrooms.save(new Classroom(floor, request.name(), request.capacity(), request.roomType())));
    }

    public record CampusRequest(@NotBlank String name, String address) {}
    public record NameRequest(@NotBlank String name) {}
    public record ClassroomRequest(@NotBlank String name, @Min(1) @Max(10000) int capacity, @NotBlank String roomType) {}
    public record CampusResponse(UUID id, String name, String address, boolean active) {
        static CampusResponse from(Campus x) { return new CampusResponse(x.getId(), x.getName(), x.getAddress(), x.isActive()); }
    }
    public record BuildingResponse(UUID id, String name, boolean active) {
        static BuildingResponse from(Building x) { return new BuildingResponse(x.getId(), x.getName(), x.isActive()); }
    }
    public record FloorResponse(UUID id, String name, boolean active) {
        static FloorResponse from(Floor x) { return new FloorResponse(x.getId(), x.getName(), x.isActive()); }
    }
    public record ClassroomResponse(UUID id, String name, int capacity, String roomType, boolean active) {
        static ClassroomResponse from(Classroom x) { return new ClassroomResponse(x.getId(), x.getName(), x.getCapacity(), x.getRoomType(), x.isActive()); }
    }
}
