package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.out.LocalLoginProfilesPort;

import io.swagger.v3.oas.annotations.Hidden;

/** Outil de développement : le login habituel continue à vérifier le mot de passe. */
@Hidden
@RestController
@Profile("local & !prod & !production & !test")
public class LocalLoginProfilesController {

	private final LocalLoginProfilesPort profiles;

	public LocalLoginProfilesController(LocalLoginProfilesPort profiles) {
		this.profiles = profiles;
	}

	@GetMapping("/api/int/auth/dev-profiles")
	public ResponseEntity<List<LocalLoginProfilesPort.Profile>> list() {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(profiles.list());
	}
}
