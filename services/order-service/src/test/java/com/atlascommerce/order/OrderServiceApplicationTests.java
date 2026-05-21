package com.atlascommerce.order;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=localhost:9092"
})
class OrderServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
