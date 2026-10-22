package com.appv4.configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Charge automatiquement le fichier {@code .env} à la racine du projet au démarrage.
 *
 * <p><b>Business :</b> en local, les secrets (Google OAuth, JWT, Postgres) restent dans
 * {@code .env} (hors git) sans coller un script PowerShell avant chaque {@code spring-boot:run}.
 *
 * <p><b>Technique :</b> {@link EnvironmentPostProcessor} exécuté très tôt. Injecte aussi
 * {@link System#setProperty} pour que les placeholders {@code ${GOOGLE_CLIENT_ID:}} du yaml
 * se résolvent. Les variables déjà présentes dans l'environnement OS / CI <b>gagnent</b>.
 *
 * <p><b>Métier / flow :</b> infra de boot — pas un port métier. Cherche {@code .env} dans
 * {@code user.dir} (répertoire d'où tu lances Maven).
 */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	private static final Log LOG = LogFactory.getLog(DotEnvEnvironmentPostProcessor.class);

	static final String PROPERTY_SOURCE_NAME = "dotenv";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Path envFile = resolveEnvFile();
		if (envFile == null) {
			LOG.debug("Aucun fichier .env trouvé (user.dir=" + System.getProperty("user.dir") + ")");
			return;
		}
		Map<String, Object> values = parseDotEnv(envFile);
		if (values.isEmpty()) {
			return;
		}
		Map<String, Object> toApply = new LinkedHashMap<>();
		for (Map.Entry<String, Object> entry : values.entrySet()) {
			String key = entry.getKey();
			if (System.getenv(key) != null) {
				continue;
			}
			if (System.getProperty(key) != null) {
				continue;
			}
			toApply.put(key, entry.getValue());
			System.setProperty(key, String.valueOf(entry.getValue()));
		}
		if (toApply.isEmpty()) {
			return;
		}
		environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, toApply));
		LOG.info("Fichier .env chargé (" + toApply.size() + " variables) : " + envFile.toAbsolutePath());
	}

	private static Path resolveEnvFile() {
		Path fromUserDir = Path.of(System.getProperty("user.dir", "."), ".env");
		if (Files.isRegularFile(fromUserDir)) {
			return fromUserDir;
		}
		Path fromCwd = Path.of(".env").toAbsolutePath().normalize();
		if (Files.isRegularFile(fromCwd)) {
			return fromCwd;
		}
		return null;
	}

	static Map<String, Object> parseDotEnv(Path envFile) {
		Map<String, Object> values = new LinkedHashMap<>();
		try {
			for (String raw : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
				String line = raw.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				int eq = line.indexOf('=');
				if (eq <= 0) {
					continue;
				}
				String key = line.substring(0, eq).trim();
				String value = line.substring(eq + 1).trim();
				if ((value.startsWith("\"") && value.endsWith("\""))
						|| (value.startsWith("'") && value.endsWith("'"))) {
					value = value.substring(1, value.length() - 1);
				}
				if (!key.isEmpty()) {
					values.put(key, value);
				}
			}
		} catch (IOException ignored) {
			// Pas de .env lisible → démarrage normal sans dotenv
		}
		return values;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
}
