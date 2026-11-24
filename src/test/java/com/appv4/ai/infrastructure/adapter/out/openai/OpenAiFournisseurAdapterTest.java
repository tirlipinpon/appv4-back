package com.appv4.ai.infrastructure.adapter.out.openai;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.appv4.ai.application.BesoinIaException;
import com.appv4.ai.application.MessageIa;

class OpenAiFournisseurAdapterTest {

	@Test
	void uneCleAbsenteNAppellePasLeReseau() {
		OpenAiFournisseurAdapter adapter = new OpenAiFournisseurAdapter("https://example.invalid", "  ");

		assertThatThrownBy(() -> adapter.completer("mini", List.of(new MessageIa("user", "bonjour"))))
				.isInstanceOf(BesoinIaException.class)
				.hasMessage(OpenAiFournisseurAdapter.CLE_ABSENTE);
	}
}
