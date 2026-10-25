package com.foody;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.springframework.boot.SpringApplication;

/**
 * Starts the app against a throw-away in-memory MongoDB on localhost:27017 (test DB "foodify_test").
 * Run with: ./run-with-test-db.sh
 *           -Dspring-boot.run.main-class=com.foody.TestDbApplication
 */
public class TestDbApplication {
    public static void main(String[] args) {
        MongoServer server = new MongoServer(new MemoryBackend());
        server.bind("localhost", 27017);
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        SpringApplication.run(FoodifyApplication.class, args);
    }
}
