package com.ykleyka.taskboard;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TaskBoardApplication {

    public static void main(String[] args) {
        configureDatabaseFromEnvironment();
        SpringApplication.run(TaskBoardApplication.class, args);
    }

    private static void configureDatabaseFromEnvironment() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (hasDatasourceUrlOverride() || databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        URI uri = URI.create(databaseUrl);
        String scheme = uri.getScheme();
        if (!"postgres".equals(scheme) && !"postgresql".equals(scheme)) {
            return;
        }

        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                .append(uri.getHost())
                .append(":")
                .append(port)
                .append(uri.getRawPath());

        if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
            jdbcUrl.append("?").append(uri.getRawQuery());
        }

        System.setProperty("spring.datasource.url", jdbcUrl.toString());

        String userInfo = uri.getRawUserInfo();
        if (userInfo != null && !userInfo.isBlank()) {
            String[] credentials = userInfo.split(":", 2);
            setPropertyIfMissing(
                    "spring.datasource.username",
                    "SPRING_DATASOURCE_USERNAME",
                    decode(credentials[0]));
            if (credentials.length > 1) {
                setPropertyIfMissing(
                        "spring.datasource.password",
                        "SPRING_DATASOURCE_PASSWORD",
                        decode(credentials[1]));
            }
        }
    }

    private static boolean hasDatasourceUrlOverride() {
        return System.getProperty("spring.datasource.url") != null
                || System.getenv("SPRING_DATASOURCE_URL") != null;
    }

    private static void setPropertyIfMissing(String propertyName, String environmentName, String value) {
        if (System.getProperty(propertyName) == null && System.getenv(environmentName) == null) {
            System.setProperty(propertyName, value);
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

}
