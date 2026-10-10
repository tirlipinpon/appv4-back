package com.appv4.hello.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.appv4.hello.application.command.GetHelloMessageCommand;
import com.appv4.hello.application.port.in.GetHelloMessageUseCase;
import com.appv4.hello.application.port.out.HelloMessageRepository;
import com.appv4.hello.application.result.HelloMessageResult;
import com.appv4.hello.domain.model.HelloAudience;
import com.appv4.hello.domain.model.HelloMessage;
import com.appv4.shared.domain.DomainException;
import com.appv4.shared.domain.ErrorCode;

class GetHelloMessageServiceTest {

	@Test
	void returnsLearningMessageFromRepository() {
		HelloMessageRepository fake = audience -> Optional.of(new HelloMessage("hello learning"));
		GetHelloMessageUseCase useCase = new GetHelloMessageService(fake);

		HelloMessageResult result = useCase.execute(new GetHelloMessageCommand(HelloAudience.LEARNING));

		assertEquals("hello learning", result.text());
	}

	@Test
	void returnsAdminMessageFromRepository() {
		HelloMessageRepository fake = audience -> Optional.of(new HelloMessage("admin-helloworld"));
		GetHelloMessageUseCase useCase = new GetHelloMessageService(fake);

		HelloMessageResult result = useCase.execute(new GetHelloMessageCommand(HelloAudience.ADMIN));

		assertEquals("admin-helloworld", result.text());
	}

	@Test
	void throwsDomainExceptionWhenMessageMissing() {
		HelloMessageRepository empty = audience -> Optional.empty();
		GetHelloMessageUseCase useCase = new GetHelloMessageService(empty);

		DomainException ex = assertThrows(DomainException.class,
				() -> useCase.execute(new GetHelloMessageCommand(HelloAudience.LEARNING)));

		assertEquals(ErrorCode.HELLO_MESSAGE_NOT_FOUND, ex.getErrorCode());
	}
}
