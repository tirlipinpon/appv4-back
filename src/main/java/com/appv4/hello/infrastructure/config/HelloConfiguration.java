package com.appv4.hello.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.appv4.hello.application.port.in.GetHelloMessageUseCase;
import com.appv4.hello.application.port.out.HelloMessageRepository;
import com.appv4.hello.application.service.GetHelloMessageService;

/**
 * Câblage Spring de la feature hello.
 * Le bean UseCase n'est créé que lorsqu'un adapter de persistance fournit {@link HelloMessageRepository}.
 */
@Configuration
public class HelloConfiguration {

	@Bean
	@ConditionalOnBean(HelloMessageRepository.class)
	GetHelloMessageUseCase getHelloMessageUseCase(HelloMessageRepository helloMessageRepository) {
		return new GetHelloMessageService(helloMessageRepository);
	}
}
