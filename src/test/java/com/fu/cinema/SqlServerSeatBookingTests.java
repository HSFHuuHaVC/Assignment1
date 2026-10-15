package com.fu.cinema;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.util.Arrays;

/** Uses only the dedicated disposable SQL Server database, never the local application database. */
@EnabledIfEnvironmentVariable(named = "CINEMA_SQLSERVER_TEST_URL", matches = ".+")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SqlServerSeatBookingTests extends SeatBookingConcurrencyTests {
    @DynamicPropertySource
    static void sqlServerProperties(DynamicPropertyRegistry properties) {
        String url = System.getenv("CINEMA_SQLSERVER_TEST_URL");
        if (url == null || !url.startsWith("jdbc:sqlserver:") || Arrays.stream(url.split(";"))
                .noneMatch(part -> part.trim().equalsIgnoreCase("databaseName=FUCinemaIssue3Test"))) {
            throw new IllegalArgumentException("SQL Server test requires databaseName=FUCinemaIssue3Test");
        }
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.driver-class-name", () -> "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        properties.add("spring.datasource.username", () -> System.getenv("CINEMA_SQLSERVER_TEST_USERNAME"));
        properties.add("spring.datasource.password", () -> System.getenv("CINEMA_SQLSERVER_TEST_PASSWORD"));
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
}
