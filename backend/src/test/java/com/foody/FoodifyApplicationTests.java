package com.foody;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

// Env placeholders from application.properties (set on Render) get empty values in tests.
@SpringBootTest(properties = {"MONGO_URI=mongodb://unused", "RAZORPAY_KEY_ID=", "RAZORPAY_SECRET=",
		"MAIL_FROM=", "MAIL_USERNAME=", "MAIL_PASSWORD="})
class FoodifyApplicationTests {

	private static final MongoServer MONGO = new MongoServer(new MemoryBackend());

	static {
		MONGO.bind("localhost", 0);
	}

	@DynamicPropertySource
	static void mongo(DynamicPropertyRegistry registry) {
		registry.add("spring.data.mongodb.uri",
				() -> "mongodb://localhost:" + MONGO.getLocalAddress().getPort() + "/foodify_test");
	}

	@AfterAll
	static void stop() {
		MONGO.shutdown();
	}

	@Test
	void contextLoads() {
	}

}
