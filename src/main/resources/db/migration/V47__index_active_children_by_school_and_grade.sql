-- Class rosters and offer lookups only include active, visible children.
CREATE INDEX idx_back_children_active_school_grade
    ON back_children (school_id, grade_label)
    WHERE active AND NOT hidden;
