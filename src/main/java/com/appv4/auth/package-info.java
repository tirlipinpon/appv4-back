/**
 * Socle authentification admin (SCRUM-93).
 *
 * <h2>Métier (ce qu'on protège)</h2>
 * L'app a deux faces :
 * <ul>
 *   <li><b>Admin</b> ({@code /api/int/**}) : parents, profs, admin — JWT obligatoire</li>
 *   <li><b>Learning</b> ({@code /api/ext/**}) : enfant — encore public (auth plus tard)</li>
 * </ul>
 * Un utilisateur = 1 email unique. Connexion future : email/mdp (BCrypt) et/ou
 * Google / Facebook / Apple (table identities).
 *
 * <h2>Technique (hexagone)</h2>
 * <pre>
 *   [Angular] --Bearer JWT--&gt; JwtAuthenticationFilter
 *                                  |
 *                                  v
 *                         JwtTokenDecoder (port)
 *                                  |
 *                                  v
 *                         JjwtTokenAdapter (JJWT)
 *                                  |
 *                                  v
 *                    SecurityContext + /api/int/** authenticated
 * </pre>
 * Émission d'un token (login à venir) : {@code IssueJwtUseCase} → encode → JWT signé.
 *
 * <h2>Base (Flyway V3 — ne pas modifier après appli)</h2>
 * {@code back_users} : compte (email, password_hash nullable si 100 % social).
 * {@code back_user_identities} : lien provider + subject → user_id.
 */
package com.appv4.auth;
