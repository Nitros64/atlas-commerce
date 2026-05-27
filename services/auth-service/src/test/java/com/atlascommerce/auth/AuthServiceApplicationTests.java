package com.atlascommerce.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.autoconfigure.exclude=org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration"
})
@ActiveProfiles("test")
class AuthServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
