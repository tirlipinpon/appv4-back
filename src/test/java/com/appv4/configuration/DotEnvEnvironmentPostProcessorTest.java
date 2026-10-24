package com.appv4.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
}
