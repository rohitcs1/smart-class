CREATE TABLE timetable_entries (
    id BINARY(16) NOT NULL,
    classroom_id BINARY(16) NOT NULL,
    teacher_id BINARY(16) NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    starts_at TIME NOT NULL,
    ends_at TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT fk_timetable_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(id),
    CONSTRAINT fk_timetable_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id)
);

CREATE TABLE merged_sessions (
    id BINARY(16) NOT NULL,
    teacher_id BINARY(16) NOT NULL,
    session_date DATE NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_merged_session_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id)
);

CREATE TABLE merged_session_classrooms (
    session_id BINARY(16) NOT NULL,
    classroom_id BINARY(16) NOT NULL,
    PRIMARY KEY (session_id, classroom_id),
    CONSTRAINT fk_merged_classroom_session FOREIGN KEY (session_id) REFERENCES merged_sessions(id),
    CONSTRAINT fk_merged_classroom_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(id)
);

CREATE TABLE attendance_records (
    id BINARY(16) NOT NULL,
    session_id BINARY(16) NOT NULL,
    teacher_id BINARY(16) NOT NULL,
    card_uid VARCHAR(100) NOT NULL,
    check_in_at DATETIME(6) NOT NULL,
    check_out_at DATETIME(6),
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_attendance_session_teacher UNIQUE (session_id, teacher_id),
    CONSTRAINT fk_attendance_session FOREIGN KEY (session_id) REFERENCES merged_sessions(id),
    CONSTRAINT fk_attendance_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id)
);

CREATE INDEX ix_timetable_classroom_day ON timetable_entries(classroom_id, day_of_week);
CREATE INDEX ix_attendance_session ON attendance_records(session_id);
