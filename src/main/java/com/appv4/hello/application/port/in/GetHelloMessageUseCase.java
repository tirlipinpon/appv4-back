package com.appv4.hello.application.port.in;

import com.appv4.hello.application.command.GetHelloMessageCommand;
import com.appv4.hello.application.result.HelloMessageResult;

/**
 * Port entrant : obtenir le message hello pour learning ou admin.
 */
public interface GetHelloMessageUseCase {

	HelloMessageResult execute(GetHelloMessageCommand command);
}
