package com.appv4.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
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
}
