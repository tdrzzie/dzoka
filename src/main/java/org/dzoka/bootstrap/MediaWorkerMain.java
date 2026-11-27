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
        if(args.length!=1 || !java.util.Set.of("sanitize","purge","expire","maintain").contains(args[0]))throw new IllegalArgumentException("Worker operation required");
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
                if(args[0].equals("maintain")) {
                    int cycles=Integer.parseInt(System.getenv().getOrDefault("DZOKA_WORKER_CYCLES","1"));
                    int interval=Integer.parseInt(System.getenv().getOrDefault("DZOKA_WORKER_INTERVAL_SECONDS","60"));
                    if(cycles<0 || cycles>10000 || interval<15 || interval>3600)throw new IllegalArgumentException("Invalid maintenance bounds");
                    boolean unresolved=false;
                    for(int cycle=0;cycles==0||cycle<cycles;cycle++) {
                        try {
                            objects.verifyPrivateBucket();
                            new JdbcReports(tx).expire(actor,tenant);
                            int processed=0;while(processed<limit&&worker.processOne(actor,tenant))processed++;
                            int purged=0;while(purged<limit&&worker.purgeOne(actor,tenant))purged++;
                            System.out.println("Media maintenance cycle completed: processed="+processed+", purged="+purged);
                            unresolved=false;
                        } catch(org.dzoka.foundation.application.Failures.Unavailable unavailable) {
                            System.err.println("Media maintenance unavailable; work remains pending");
                            unresolved=true;
                            if(cycles==1)throw new IllegalStateException("Media maintenance did not complete");
                        }
                        if(cycles==0||cycle+1<cycles)Thread.sleep(interval*1000L);
                    }
                    if(unresolved)throw new IllegalStateException("Media maintenance did not complete");
                    return;
                }
                int count=0;while(count<limit && (args[0].equals("sanitize")?worker.processOne(actor,tenant):worker.purgeOne(actor,tenant)))count++;
                System.out.println("Processed media operations: "+count);
            }
        }
    }
}
