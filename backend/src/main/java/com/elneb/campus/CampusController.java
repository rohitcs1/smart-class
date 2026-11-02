package com.elneb.campus;

import com.elneb.institution.Institution;
import com.elneb.institution.InstitutionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import com.elneb.security.TenantAccessService;
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
    private final TenantAccessService access;

    public CampusController(CampusRepository campuses, BuildingRepository buildings, FloorRepository floors,
                            ClassroomRepository classrooms, InstitutionRepository institutions, TenantAccessService access) {
        this.campuses = campuses; this.buildings = buildings; this.floors = floors;
        this.classrooms = classrooms; this.institutions = institutions; this.access = access;
    }

    @GetMapping("/institution/{institutionId}")
    public List<CampusResponse> listCampuses(@PathVariable UUID institutionId, Authentication authentication) {
        access.requireInstitution(institutionId, authentication);
        return campuses.findAllByInstitutionIdOrderByName(institutionId).stream().map(CampusResponse::from).toList();
    }

    @PostMapping("/institution/{institutionId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CampusResponse createCampus(@PathVariable UUID institutionId, @Valid @RequestBody CampusRequest request, Authentication authentication) {
        access.requireInstitution(institutionId, authentication);
        if (campuses.existsByInstitutionIdAndNameIgnoreCase(institutionId, request.name())) throw new IllegalArgumentException("Campus name is already in use");
        Institution institution = institutions.findById(institutionId).orElseThrow(() -> new IllegalArgumentException("Institution not found"));
        return CampusResponse.from(campuses.save(new Campus(institution, request.name(), request.address())));
    }

    @PutMapping("/{campusId}")
    public CampusResponse updateCampus(@PathVariable UUID campusId, @Valid @RequestBody CampusRequest request, Authentication authentication) {
        Campus campus = campuses.findById(campusId).orElseThrow(() -> new IllegalArgumentException("Campus not found"));
        access.requireCampus(campus, authentication); campus.update(request.name(), request.address());
        return CampusResponse.from(campuses.save(campus));
    }

    @DeleteMapping("/{campusId}")
    public void deleteCampus(@PathVariable UUID campusId, Authentication authentication) {
        Campus campus = campuses.findById(campusId).orElseThrow(() -> new IllegalArgumentException("Campus not found"));
        access.requireCampus(campus, authentication); campus.setActive(false); campuses.save(campus);
    }

    @GetMapping("/{campusId}/buildings")
    public List<BuildingResponse> listBuildings(@PathVariable UUID campusId, Authentication authentication) {
        access.requireCampus(campuses.findById(campusId).orElseThrow(() -> new IllegalArgumentException("Campus not found")), authentication);
        return buildings.findAllByCampusIdOrderByName(campusId).stream().map(BuildingResponse::from).toList();
    }

    @PostMapping("/{campusId}/buildings")
    @ResponseStatus(HttpStatus.CREATED)
    public BuildingResponse createBuilding(@PathVariable UUID campusId, @Valid @RequestBody NameRequest request, Authentication authentication) {
        Campus campus = campuses.findById(campusId).orElseThrow(() -> new IllegalArgumentException("Campus not found")); access.requireCampus(campus, authentication);
        if (buildings.existsByCampusIdAndNameIgnoreCase(campusId, request.name())) throw new IllegalArgumentException("Building name is already in use");
        return BuildingResponse.from(buildings.save(new Building(campus, request.name())));
    }

    @PutMapping("/buildings/{buildingId}")
    public BuildingResponse updateBuilding(@PathVariable UUID buildingId, @Valid @RequestBody NameRequest request, Authentication authentication) {
        Building building = buildings.findById(buildingId).orElseThrow(() -> new IllegalArgumentException("Building not found"));
        access.requireBuilding(building, authentication); building.update(request.name()); return BuildingResponse.from(buildings.save(building));
    }

    @DeleteMapping("/buildings/{buildingId}")
    public void deleteBuilding(@PathVariable UUID buildingId, Authentication authentication) {
        Building building = buildings.findById(buildingId).orElseThrow(() -> new IllegalArgumentException("Building not found"));
        access.requireBuilding(building, authentication); building.setActive(false); buildings.save(building);
    }

    @GetMapping("/buildings/{buildingId}/floors")
    public List<FloorResponse> listFloors(@PathVariable UUID buildingId, Authentication authentication) {
        access.requireBuilding(buildings.findById(buildingId).orElseThrow(() -> new IllegalArgumentException("Building not found")), authentication);
        return floors.findAllByBuildingIdOrderByName(buildingId).stream().map(FloorResponse::from).toList();
    }

    @PostMapping("/buildings/{buildingId}/floors")
    @ResponseStatus(HttpStatus.CREATED)
    public FloorResponse createFloor(@PathVariable UUID buildingId, @Valid @RequestBody NameRequest request, Authentication authentication) {
        Building building = buildings.findById(buildingId).orElseThrow(() -> new IllegalArgumentException("Building not found")); access.requireBuilding(building, authentication);
        if (floors.existsByBuildingIdAndNameIgnoreCase(buildingId, request.name())) throw new IllegalArgumentException("Floor name is already in use");
        return FloorResponse.from(floors.save(new Floor(building, request.name())));
    }

    @PutMapping("/floors/{floorId}")
    public FloorResponse updateFloor(@PathVariable UUID floorId, @Valid @RequestBody NameRequest request, Authentication authentication) {
        Floor floor = floors.findById(floorId).orElseThrow(() -> new IllegalArgumentException("Floor not found"));
        access.requireFloor(floor, authentication); floor.update(request.name()); return FloorResponse.from(floors.save(floor));
    }

    @DeleteMapping("/floors/{floorId}")
    public void deleteFloor(@PathVariable UUID floorId, Authentication authentication) {
        Floor floor = floors.findById(floorId).orElseThrow(() -> new IllegalArgumentException("Floor not found"));
        access.requireFloor(floor, authentication); floor.setActive(false); floors.save(floor);
    }

    @GetMapping("/floors/{floorId}/classrooms")
    public List<ClassroomResponse> listClassrooms(@PathVariable UUID floorId, Authentication authentication) {
        access.requireFloor(floors.findById(floorId).orElseThrow(() -> new IllegalArgumentException("Floor not found")), authentication);
        return classrooms.findAllByFloorIdOrderByName(floorId).stream().map(ClassroomResponse::from).toList();
    }

    @PostMapping("/floors/{floorId}/classrooms")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassroomResponse createClassroom(@PathVariable UUID floorId, @Valid @RequestBody ClassroomRequest request, Authentication authentication) {
        Floor floor = floors.findById(floorId).orElseThrow(() -> new IllegalArgumentException("Floor not found")); access.requireFloor(floor, authentication);
        if (classrooms.existsByFloorIdAndNameIgnoreCase(floorId, request.name())) throw new IllegalArgumentException("Classroom name is already in use");
        return ClassroomResponse.from(classrooms.save(new Classroom(floor, request.name(), request.capacity(), request.roomType())));
    }

    @PutMapping("/classrooms/{classroomId}")
    public ClassroomResponse updateClassroom(@PathVariable UUID classroomId, @Valid @RequestBody ClassroomRequest request, Authentication authentication) {
        Classroom room = classrooms.findById(classroomId).orElseThrow(() -> new IllegalArgumentException("Classroom not found"));
        access.requireClassroom(room, authentication); room.update(request.name(), request.capacity(), request.roomType());
        return ClassroomResponse.from(classrooms.save(room));
    }

    @PatchMapping("/classrooms/{classroomId}/active")
    public ClassroomResponse setClassroomActive(@PathVariable UUID classroomId, @RequestParam boolean active, Authentication authentication) {
        Classroom room = classrooms.findById(classroomId).orElseThrow(() -> new IllegalArgumentException("Classroom not found"));
        access.requireClassroom(room, authentication); room.setActive(active); return ClassroomResponse.from(classrooms.save(room));
    }

    @DeleteMapping("/classrooms/{classroomId}")
    public void deleteClassroom(@PathVariable UUID classroomId, Authentication authentication) {
        Classroom room = classrooms.findById(classroomId).orElseThrow(() -> new IllegalArgumentException("Classroom not found"));
        access.requireClassroom(room, authentication); room.setActive(false); classrooms.save(room);
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
