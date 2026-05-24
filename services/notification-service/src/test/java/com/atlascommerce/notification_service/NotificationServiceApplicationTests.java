package com.atlascommerce.notification_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false"
})
class NotificationServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
