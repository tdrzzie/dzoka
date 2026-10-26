package org.dzoka.bootstrap;
import java.util.UUID;
import com.zaxxer.hikari.*;
import org.dzoka.foundation.persistence.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.intake.adapters.JdbcReports;
import org.dzoka.media.application.MediaWorker;
import org.dzoka.media.adapters.*;

/** Trusted local core worker. Service identity is operator configuration, not an untrusted job field. */
public final class MediaWorkerMain {
    private static String required(String name){String v=System.getenv(name);if(v==null||v.isBlank())throw new IllegalArgumentException("Missing configuration: "+name);return v;}
    public static void main(String[] args) throws Exception {
        if(args.length!=1 || !java.util.Set.of("sanitize","purge","expire").contains(args[0]))throw new IllegalArgumentException("Worker operation required");
        var config=new HikariConfig();config.setJdbcUrl(required("DZOKA_DB_URL"));config.setUsername(required("DZOKA_DB_USER"));
        config.setPassword(required("DZOKA_DB_PASSWORD"));config.setMaximumPoolSize(2);
        try(var data=new HikariDataSource(config)){
            new RuntimeDatabaseGuard(data).afterPropertiesSet();var tx=new TenantTransactions(data);
            var actor=new VerifiedIdentity(required("DZOKA_WORKER_ISSUER"),required("DZOKA_WORKER_SUBJECT"));UUID tenant=UUID.fromString(required("DZOKA_WORKER_TENANT"));
            if(args[0].equals("expire")){System.out.println("Expired reports: "+new JdbcReports(tx).expire(actor,tenant));return;}
            try(var objects=new S3PrivateObjects(required("DZOKA_MEDIA_ENDPOINT"),required("DZOKA_MEDIA_REGION"),required("DZOKA_MEDIA_BUCKET"),
                    required("DZOKA_MEDIA_ACCESS_KEY"),required("DZOKA_MEDIA_SECRET_KEY"),Boolean.parseBoolean(System.getenv("DZOKA_MEDIA_ALLOW_INSECURE_LOOPBACK")))){
                objects.verifyPrivateBucket();
                var worker=new MediaWorker(new JdbcMediaStore(tx),objects,new DockerRasterDecoder());
                int limit=Integer.parseInt(System.getenv().getOrDefault("DZOKA_WORKER_LIMIT","10"));if(limit<1||limit>100)throw new IllegalArgumentException("Invalid worker limit");
                int count=0;while(count<limit && (args[0].equals("sanitize")?worker.processOne(actor,tenant):worker.purgeOne(actor,tenant)))count++;
                System.out.println("Processed media operations: "+count);
            }
        }
    }
}
