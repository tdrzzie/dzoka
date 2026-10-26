package org.dzoka.foundation.persistence;

import org.flywaydb.core.Flyway;

/** Separate operational entry point. Migration credentials are never API properties. */
public final class MigrateDatabase {
    private MigrateDatabase() {}
    public static void main(String[] args) {
        Flyway.configure().dataSource(required("DZOKA_MIGRATION_URL"),
                required("DZOKA_MIGRATION_USER"), required("DZOKA_MIGRATION_PASSWORD"))
                .locations("classpath:db/migration").cleanDisabled(true).load().migrate();
    }
    private static String required(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Required configuration: " + key);
        return value;
    }
}
