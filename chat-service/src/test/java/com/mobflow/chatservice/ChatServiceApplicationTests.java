package com.mobflow.chatservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.mobflow.chatservice.testsupport.AbstractMongoChatTest;

@SpringBootTest(properties = "security.jwt.secret-key=Y2hhdC1zZXJ2aWNlLXRlc3Qtc2VjcmV0LWtleS1jaGF0LXNlcnZpY2UtdGVzdA==")
class ChatServiceApplicationTests extends AbstractMongoChatTest {

	@Test
	void contextLoads() {
	}

}
