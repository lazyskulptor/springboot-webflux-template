package my.lazyskulptor.commerce;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

public class ContainerExtension implements BeforeAllCallback {

    private static final AtomicBoolean started = new AtomicBoolean(false);
    private static final MySQLContainer<?> container = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.28"))
            .withDatabaseName("commerce")
            .withTmpFs(Collections.singletonMap("/testtmpfs", "rw"));

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!started.get()) {
            container.start();
            System.setProperty("spring.datasource.driver-class-name", container.getDriverClassName());
            System.setProperty("spring.datasource.url", container.getJdbcUrl());
            System.setProperty("spring.datasource.username", container.getUsername());
            System.setProperty("spring.datasource.password", container.getPassword());
            started.set(true);
        }
    }
}
