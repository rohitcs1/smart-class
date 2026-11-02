CREATE TABLE teachers (
    id BINARY(16) NOT NULL,
    institution_id BINARY(16) NOT NULL,
    employee_code VARCHAR(80) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(255),
    department VARCHAR(150),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_teachers_institution_code UNIQUE (institution_id, employee_code),
    CONSTRAINT fk_teachers_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE TABLE rfid_cards (
    id BINARY(16) NOT NULL,
    teacher_id BINARY(16) NOT NULL,
    card_uid VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_rfid_cards_uid UNIQUE (card_uid),
    CONSTRAINT uk_rfid_cards_teacher UNIQUE (teacher_id),
    CONSTRAINT fk_rfid_cards_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id)
);

CREATE INDEX ix_teachers_institution ON teachers(institution_id);
