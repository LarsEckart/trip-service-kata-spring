package kata;

import org.springframework.boot.SpringApplication;

/**
 * Application runner for local development using testcontainers.
 * 
 * This starts the application with a PostgreSQL testcontainer instead of requiring
 * a separate database installation. Useful for local development and testing.
 * 
 * Usage: Run this main method to start the application locally with testcontainers.
 */
public class LocalDevApplication {

    public static void main(String[] args) {
        SpringApplication.from(App::main)
                .with(TestContainersConfiguration.class)
                .run(args);
    }
}
