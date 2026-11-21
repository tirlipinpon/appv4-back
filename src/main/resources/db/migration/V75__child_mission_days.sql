-- Jour où la mission a été finie, sans ses tâches (BACK-59).
CREATE TABLE back_child_mission_days (
    child_id     BIGINT NOT NULL REFERENCES back_children (id) ON DELETE CASCADE,
    mission_date DATE NOT NULL,
    PRIMARY KEY (child_id, mission_date)
);

COMMENT ON TABLE back_child_mission_days IS
    'Jour Europe/Paris où l''enfant a fini sa mission. Survit à la suppression d''une question (BACK-59).';
