package org.dzoka.operations;

import java.time.Instant;
import java.util.*;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.learning.Learning;
import org.springframework.stereotype.Service;

@Service
public final class JobDiagnostics {
    private final TenantTransactions tx;private final Telemetry telemetry;
    public JobDiagnostics(TenantTransactions tx,Telemetry telemetry){this.tx=tx;this.telemetry=telemetry;}
    private record Span(String parent,String task,String state,Instant start,Instant end) {}
    public int export(VerifiedIdentity actor,UUID tenant){
        if(!telemetry.enabled())throw new Failures.Conflict();
        var spans=tx.execute(actor,tenant,"telemetry:export",db->{var values=new ArrayList<Span>();try(var s=Learning.q(db,"SELECT * FROM dzoka.export_job_diagnostics()");var r=s.executeQuery()){while(r.next())values.add(new Span(r.getString(1),r.getString(2),r.getString(3),r.getTimestamp(4).toInstant(),r.getTimestamp(5).toInstant()));}return List.copyOf(values);});
        // Diagnostics may be lost at process death or collector failure; domain results remain committed.
        for(var span:spans)telemetry.job(span.parent(),span.task(),span.state(),span.start(),span.end());return spans.size();
    }
}
