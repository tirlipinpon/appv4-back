package com.appv4.hello.infrastructure.adapter.in.web.intapi;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.hello.application.command.GetHelloMessageCommand;
import com.appv4.hello.application.port.in.GetHelloMessageUseCase;
import com.appv4.hello.application.result.HelloMessageResult;
import com.appv4.hello.domain.model.HelloAudience;
import com.appv4.hello.infrastructure.adapter.in.web.HelloMessageResponse;

/**
 * API admin — {@code GET /api/int/hello}.
 */
@RestController
@RequestMapping("/api/int")
public class IntHelloController {

	private final GetHelloMessageUseCase getHelloMessageUseCase;

	public IntHelloController(GetHelloMessageUseCase getHelloMessageUseCase) {
		this.getHelloMessageUseCase = getHelloMessageUseCase;
	}

	@GetMapping("/hello")
	public ResponseEntity<HelloMessageResponse> hello() {
		HelloMessageResult result = getHelloMessageUseCase.execute(
				new GetHelloMessageCommand(HelloAudience.ADMIN));
		return ResponseEntity.ok(new HelloMessageResponse(result.text()));
	}
}
