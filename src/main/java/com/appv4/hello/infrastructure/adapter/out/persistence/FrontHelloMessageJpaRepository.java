package com.appv4.hello.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface FrontHelloMessageJpaRepository extends JpaRepository<FrontHelloMessageEntity, Long> {

	Optional<FrontHelloMessageEntity> findFirstByOrderByIdDesc();
}
