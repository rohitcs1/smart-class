ALTER TABLE classrooms ADD COLUMN camera_url VARCHAR(1000);

CREATE TABLE camera_permissions (
    id BINARY(16) NOT NULL,
    classroom_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_camera_permission UNIQUE (classroom_id, user_id),
    CONSTRAINT fk_camera_permission_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(id),
    CONSTRAINT fk_camera_permission_user FOREIGN KEY (user_id) REFERENCES users(id)
);
