package org.dzoka.bootstrap;
import java.util.UUID;
import com.zaxxer.hikari.*;
import org.dzoka.foundation.persistence.*;
import org.dzoka.fingerprints.adapters.JdbcFingerprintJobs;
import org.dzoka.fingerprints.adapters.JdbcPerceptionJobs;
import org.dzoka.identity.domain.VerifiedIdentity;

/** Trusted maintenance entry point; the private inference client never receives these credentials. */
public final class AiMaintenanceMain {
    private static String required(String name){String v=System.getenv(name);if(v==null||v.isBlank())throw new IllegalArgumentException("Missing configuration: "+name);return v;}
    public static void main(String[] args) throws Exception {
        var config=new HikariConfig();config.setJdbcUrl(required("DZOKA_DB_URL"));config.setUsername(required("DZOKA_DB_USER"));config.setPassword(required("DZOKA_DB_PASSWORD"));config.setMaximumPoolSize(2);
        try(var data=new HikariDataSource(config)){
            new RuntimeDatabaseGuard(data).afterPropertiesSet();
            var actor=new VerifiedIdentity(required("DZOKA_WORKER_ISSUER"),required("DZOKA_WORKER_SUBJECT"));
            System.out.println("Erased AI components: "+new JdbcFingerprintJobs(new TenantTransactions(data)).purge(actor,UUID.fromString(required("DZOKA_WORKER_TENANT"))));
            System.out.println("Erased perception components: "+new JdbcPerceptionJobs(new TenantTransactions(data),null).purge(actor,UUID.fromString(required("DZOKA_WORKER_TENANT"))));
        }
    }
}
