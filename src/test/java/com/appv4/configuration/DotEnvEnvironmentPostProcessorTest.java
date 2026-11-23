package com.appv4.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class DotEnvEnvironmentPostProcessorTest {

	@Test
	void parseIgnoresCommentsAndSupportsQuotes(@TempDir Path dir) throws Exception {
		Path env = dir.resolve(".env");
		Files.writeString(env, """
				# comment
				FOO=bar
				QUOTED="hello world"
				EMPTY=
				NOEQ
				""");

		Map<String, Object> values = DotEnvEnvironmentPostProcessor.parseDotEnv(env);

		assertThat(values)
				.containsEntry("FOO", "bar")
				.containsEntry("QUOTED", "hello world")
				.containsEntry("EMPTY", "")
				.doesNotContainKey("NOEQ");
	}

	@Test
	void springBootTestStackSkipsPersonalDotEnv() {
		assertThat(DotEnvEnvironmentPostProcessor.launchedBySpringTest(List.of(
				"org.springframework.boot.test.context.SpringBootContextLoader",
				"java.lang.Thread"))).isTrue();
	}

	@Test
	void applicationMainStillReadsDotEnv() {
		assertThat(DotEnvEnvironmentPostProcessor.launchedBySpringTest(List.of(
				"org.springframework.boot.SpringApplication",
				"com.appv4.Appv4BackApplication"))).isFalse();
	}

	@Test
	void attachDotEnvDoesNotMutateJvmSystemProperties() {
		String marker = "APPV4_DOTENV_LEAK_CHECK_" + System.nanoTime();
		String previousProfile = System.getProperty("spring.profiles.active");
		assertThat(System.getProperty(marker)).isNull();

		StandardEnvironment environment = new StandardEnvironment();
		Map<String, Object> values = new LinkedHashMap<>();
		values.put(marker, "from-dotenv");
		values.put("SPRING_PROFILES_ACTIVE", "local");
		values.put("spring.profiles.active", "local");

		DotEnvEnvironmentPostProcessor.attachDotEnvSource(environment.getPropertySources(), values);

		assertThat(System.getProperty(marker)).isNull();
		assertThat(System.getProperty("spring.profiles.active")).isEqualTo(previousProfile);
		assertThat(environment.getProperty(marker)).isEqualTo("from-dotenv");
		assertThat(environment.getProperty("spring.profiles.active")).isEqualTo("local");
		assertThat(environment.getPropertySources().get(DotEnvEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
				.isInstanceOf(MapPropertySource.class);
	}
}
