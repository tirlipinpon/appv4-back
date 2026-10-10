package com.appv4.hello.application.service;

import java.util.Objects;

import com.appv4.hello.application.command.GetHelloMessageCommand;
import com.appv4.hello.application.port.in.GetHelloMessageUseCase;
import com.appv4.hello.application.port.out.HelloMessageRepository;
import com.appv4.hello.application.result.HelloMessageResult;
import com.appv4.hello.domain.model.HelloMessage;
import com.appv4.shared.domain.DomainException;
import com.appv4.shared.domain.ErrorCode;

/**
 * Use case : obtenir le message hello pour learning ou admin.
 * Aucune dépendance Spring / HTTP / JPA — uniquement les ports.
 */
public class GetHelloMessageService implements GetHelloMessageUseCase {

	private final HelloMessageRepository helloMessageRepository;

	public GetHelloMessageService(HelloMessageRepository helloMessageRepository) {
		this.helloMessageRepository = Objects.requireNonNull(helloMessageRepository, "helloMessageRepository");
	}

	@Override
	public HelloMessageResult execute(GetHelloMessageCommand command) {
		Objects.requireNonNull(command, "command");
		HelloMessage message = helloMessageRepository
				.findLatest(command.audience())
				.orElseThrow(() -> new DomainException(
						ErrorCode.HELLO_MESSAGE_NOT_FOUND,
						"No hello message found for audience: " + command.audience()));
		return new HelloMessageResult(message.text());
	}
}
