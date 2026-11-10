package com.appv4.auth.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest(showSql = false, properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("persistence-test")
@EnabledIfEnvironmentVariable(named = "APPV4_PERSISTENCE_TEST_URL", matches = ".+")
class StudentSummaryPersistenceTest {
    @Autowired BackChildJpaRepository children;
    @Autowired JdbcTemplate jdbc;
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("APPV4_PERSISTENCE_TEST_URL"));
        registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_USER", "appv4"));
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_PASSWORD", "appv4"));
    }
    @Test
    void returnsPhotoPresenceForActiveStudentsOnly() {
        long user = jdbc.queryForObject("insert into back_users (email, display_name) values (?, 'Test') returning id",
                Long.class, UUID.randomUUID() + "@example.test");
        long school = jdbc.queryForObject("insert into back_schools (name, country, teaching_types) "
                + "values (?, 'France', 'Primaire') returning id", Long.class, UUID.randomUUID().toString());
        String columns = "insert into back_children (parent_user_id, first_name, last_name, sex, age, "
                + "curriculum, grade_label, school_id, photo, avatar_code, pin_hash) ";
        jdbc.update(columns + "values (?, 'Alice', 'Test', 'FILLE', 8, 'FRANCE', 'CE1', ?, ?, null, 'hash')",
                user, school, new byte[]{1, 2, 3});
        jdbc.update(columns + "values (?, 'Bob', 'Test', 'GARCON', 8, 'FRANCE', 'CE1', ?, null, 'cat', 'hash')",
                user, school);
        var rows = children.findStudentSummaries(List.of(school));
        assertThat(rows).hasSize(2);
        assertThat(rows).filteredOn(row -> row.getFirstName().equals("Alice")).allMatch(BackChildJpaRepository.StudentSummary::getHasPhoto);
        assertThat(rows).filteredOn(row -> row.getFirstName().equals("Bob")).noneMatch(BackChildJpaRepository.StudentSummary::getHasPhoto);
        var parentRows = children.findParentSummaries(user);
        assertThat(parentRows).hasSize(2);
        assertThat(parentRows.get(0).getHasPhoto()).isTrue();
        assertThat(parentRows.get(1).getHasPhoto()).isFalse();
        assertThat(parentRows).allMatch(BackChildJpaRepository.ChildSummary::getActive);
        assertThat(children.findParentSummaries(-1)).isEmpty();
    }
}
