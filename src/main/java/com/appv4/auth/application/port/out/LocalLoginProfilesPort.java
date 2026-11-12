package com.appv4.auth.application.port.out;

import java.util.List;

/** Catalogue des comptes pour le sélecteur de connexion local, sans secrets. */
public interface LocalLoginProfilesPort {

	List<Profile> list();

	record Profile(long id, String email, String displayName, String roles, boolean active) { }
}
