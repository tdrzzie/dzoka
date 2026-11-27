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
    static String required(String key) {
        String value = System.getenv(key);
        String file=System.getenv(key+"_FILE");
        if(file!=null&&!file.isBlank()) {
            if(value!=null&&!value.isBlank())throw new IllegalArgumentException("Conflicting configuration: "+key);
            try{var path=java.nio.file.Path.of(file);if(java.nio.file.Files.size(path)>8192)throw new IllegalArgumentException("Invalid secret file");value=java.nio.file.Files.readString(path).strip();}
            catch(java.io.IOException unavailable){throw new IllegalArgumentException("Required secret file unavailable: "+key);}
        }
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Required configuration: " + key);
        return value;
    }
}
