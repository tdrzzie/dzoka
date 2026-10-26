package org.dzoka.intake.adapters;

import java.sql.*;
import java.time.*;
import java.util.*;
import org.dzoka.foundation.application.*;
import org.dzoka.foundation.persistence.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.intake.domain.*;
import org.dzoka.intake.ports.Reports;
import org.springframework.stereotype.Component;

@Component
public final class JdbcReports implements Reports {
    private final TenantTransactions tx;
    public JdbcReports(TenantTransactions tx){this.tx=tx;}
    @Override public ReportReceipt submit(VerifiedIdentity actor,UUID tenant,UUID target,Integer expected,
            ReportDraft draft,byte[] raw,String key){
        String operation=target==null?"report.create.v1":"report.revise.v1:"+target+":"+expected;
        String digest=ContentDigest.sha256(raw),keyHash=VerifiedIdentity.digest(key);
        return tx.execute(actor,tenant,"reports:write",c->{
            try(var s=c.prepareStatement("INSERT INTO dzoka.intake_commands(tenant_id,actor_ref,operation,key_hash,request_hash) VALUES(?,?,?,?,?) ON CONFLICT DO NOTHING")){
                s.setObject(1,tenant);s.setString(2,actor.reference());s.setString(3,operation);s.setString(4,keyHash);s.setString(5,digest);s.executeUpdate();
            }
            try(var s=c.prepareStatement("SELECT * FROM dzoka.intake_commands WHERE tenant_id=? AND actor_ref=? AND operation=? AND key_hash=? FOR UPDATE")){
                s.setObject(1,tenant);s.setString(2,actor.reference());s.setString(3,operation);s.setString(4,keyHash);
                try(var r=s.executeQuery()){
                    if(!r.next())throw new Failures.Forbidden();
                    if(!digest.equals(r.getString("request_hash")))throw new Failures.Conflict();
                    UUID report=r.getObject("report_id",UUID.class);
                    if(report!=null){
                        activeReport(c,tenant,report,actor,false);
                        return new ReportReceipt(r.getObject("case_id",UUID.class),r.getObject("item_id",UUID.class),report,r.getInt("result_revision"));
                    }
                }
            }
            UUID caseId,itemId,reportId;int version;long aggregateVersion;
            if(target==null){
                caseId=UUID.randomUUID();itemId=UUID.randomUUID();reportId=UUID.randomUUID();version=1;aggregateVersion=1;
                try(var s=c.prepareStatement("INSERT INTO dzoka.recovery_cases(tenant_id,id) VALUES(?,?)")){
                    s.setObject(1,tenant);s.setObject(2,caseId);s.executeUpdate();
                }
                try(var s=c.prepareStatement("INSERT INTO dzoka.items(tenant_id,id,case_id) VALUES(?,?,?)")){
                    s.setObject(1,tenant);s.setObject(2,itemId);s.setObject(3,caseId);s.executeUpdate();
                }
                try(var s=c.prepareStatement("INSERT INTO dzoka.reports(tenant_id,id,case_id,item_id,owner_ref,kind,current_revision,expires_at) VALUES(?,?,?,?,?,?,1,now()+((SELECT report_retention_days FROM dzoka.tenants WHERE id=?)*interval '1 day'))")){
                    s.setObject(1,tenant);s.setObject(2,reportId);s.setObject(3,caseId);s.setObject(4,itemId);
                    s.setString(5,actor.reference());s.setString(6,draft.kind().name());s.setObject(7,tenant);s.executeUpdate();
                }
            }else{
                ReportRow r=activeReport(c,tenant,target,actor,true);
                if(r.version()!=expected || !r.kind().equals(draft.kind().name()))throw new Failures.Conflict();
                caseId=r.caseId();itemId=r.itemId();reportId=target;version=r.version()+1;aggregateVersion=r.aggregateVersion()+1;
                try(var s=c.prepareStatement("UPDATE dzoka.reports SET current_revision=?,version=version+1 WHERE tenant_id=? AND id=?")){
                    s.setInt(1,version);s.setObject(2,tenant);s.setObject(3,target);if(s.executeUpdate()!=1)throw new Failures.Conflict();
                }
            }
            try(var s=c.prepareStatement("""
                INSERT INTO dzoka.report_revisions(tenant_id,report_id,revision,raw_request,raw_sha256,raw_text,
                 normalized_text,normalization_version,language,timezone,raw_location,raw_time,latitude,longitude,
                 uncertainty_meters,occurred_after,occurred_before,actor_ref)
                VALUES(?,?,?,?,?,?,?,'unicode-nfc-whitespace-v1',?,?,?,?,?,?,?,?,?,?)
                """)){
                s.setObject(1,tenant);s.setObject(2,reportId);s.setInt(3,version);s.setBytes(4,raw);s.setString(5,digest);
                s.setString(6,draft.rawText());s.setString(7,draft.normalizedText());s.setString(8,draft.language());s.setString(9,draft.timezone());
                s.setString(10,draft.rawLocation());s.setString(11,draft.rawTime());s.setObject(12,draft.latitude());s.setObject(13,draft.longitude());
                s.setObject(14,draft.uncertaintyMeters());s.setTimestamp(15,timestamp(draft.occurredAfter()));s.setTimestamp(16,timestamp(draft.occurredBefore()));
                s.setString(17,actor.reference());s.executeUpdate();
            }
            try(var s=c.prepareStatement("UPDATE dzoka.intake_commands SET report_id=?,case_id=?,item_id=?,result_revision=? WHERE tenant_id=? AND actor_ref=? AND operation=? AND key_hash=?")){
                s.setObject(1,reportId);s.setObject(2,caseId);s.setObject(3,itemId);s.setInt(4,version);s.setObject(5,tenant);
                s.setString(6,actor.reference());s.setString(7,operation);s.setString(8,keyHash);s.executeUpdate();
            }
            MutationJournal.append(c,tenant,actor,target==null?"report.submitted":"report.revised",reportId,aggregateVersion,"report_id");
            return new ReportReceipt(caseId,itemId,reportId,version);
        });
    }
    private record ReportRow(UUID caseId,UUID itemId,String kind,int version,long aggregateVersion,boolean held){}
    private static ReportRow activeReport(Connection c,UUID tenant,UUID id,VerifiedIdentity actor,boolean write) throws SQLException {
        try(var s=c.prepareStatement("""
            SELECT *, (owner_ref=? OR dzoka.permitted('reports:write:any')) AS writable
            FROM dzoka.reports WHERE tenant_id=? AND id=? AND deleted_at IS NULL AND expires_at>clock_timestamp()
            """+(write?" FOR UPDATE":""))){
            s.setString(1,actor.reference());s.setObject(2,tenant);s.setObject(3,id);
            try(var r=s.executeQuery()){
                if(!r.next())throw new Failures.NotFound();
                if(write && !r.getBoolean("writable"))throw new Failures.Forbidden();
                return new ReportRow(r.getObject("case_id",UUID.class),r.getObject("item_id",UUID.class),r.getString("kind"),r.getInt("current_revision"),r.getLong("version"),r.getBoolean("legal_hold"));
            }
        }
    }
    @Override public ReportView get(VerifiedIdentity actor,UUID tenant,UUID report,Integer revision){
        return tx.execute(actor,tenant,"reports:read",c->{
            ReportRow header=activeReport(c,tenant,report,actor,false);
            try(var s=c.prepareStatement("""
                SELECT v.*,r.expires_at,r.legal_hold FROM dzoka.report_revisions v JOIN dzoka.reports r
                ON r.tenant_id=v.tenant_id AND r.id=v.report_id WHERE v.tenant_id=? AND v.report_id=? AND v.revision=?
                """)){
                s.setObject(1,tenant);s.setObject(2,report);s.setInt(3,revision==null?header.version():revision);
                try(var r=s.executeQuery()){
                    if(!r.next())throw new Failures.NotFound();
                    return new ReportView(header.caseId(),header.itemId(),report,r.getInt("revision"),ReportDraft.Kind.valueOf(header.kind()),
                        r.getString("raw_text"),r.getString("normalized_text"),r.getString("normalization_version"),r.getString("language"),r.getString("timezone"),
                        r.getString("raw_location"),r.getString("raw_time"),(Double)r.getObject("latitude"),(Double)r.getObject("longitude"),(Double)r.getObject("uncertainty_meters"),
                        instant(r.getTimestamp("occurred_after")),instant(r.getTimestamp("occurred_before")),r.getTimestamp("expires_at").toInstant(),r.getBoolean("legal_hold"));
                }
            }
        });
    }
    @Override public IntakeReference reference(VerifiedIdentity actor,UUID tenant,UUID id,boolean caseLookup){
        return tx.execute(actor,tenant,"reports:read",c->{
            try(var s=c.prepareStatement("""
                SELECT r.case_id,r.item_id,r.id,c.state,i.identity_state FROM dzoka.reports r
                JOIN dzoka.recovery_cases c ON c.tenant_id=r.tenant_id AND c.id=r.case_id
                JOIN dzoka.items i ON i.tenant_id=r.tenant_id AND i.id=r.item_id
                WHERE r.tenant_id=? AND r.deleted_at IS NULL AND r.expires_at>clock_timestamp() AND
                """+(caseLookup?"r.case_id=?":"r.item_id=?"))){
                s.setObject(1,tenant);s.setObject(2,id);try(var r=s.executeQuery()){
                    if(!r.next())throw new Failures.NotFound();return new IntakeReference(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getObject(3,UUID.class),r.getString(4),r.getString(5));
                }
            }
        });
    }
    @Override public void delete(VerifiedIdentity actor,UUID tenant,UUID report){
        tx.execute(actor,tenant,"reports:write",c->{
            ReportRow row=activeReport(c,tenant,report,actor,true);
            if(row.held())throw new Failures.Conflict();
            tombstone(c,tenant,report);
            MutationJournal.append(c,tenant,actor,"report.deleted",report,row.aggregateVersion()+1,"report_id");return null;
        });
    }
    @Override public void hold(VerifiedIdentity actor,UUID tenant,UUID report,boolean held){
        tx.execute(actor,tenant,"reports:hold",c->{
            long aggregateVersion;
            try(var s=c.prepareStatement("SELECT version,legal_hold FROM dzoka.reports WHERE tenant_id=? AND id=? AND deleted_at IS NULL FOR UPDATE")){
                s.setObject(1,tenant);s.setObject(2,report);try(var r=s.executeQuery()){
                    if(!r.next())throw new Failures.NotFound();
                    if(r.getBoolean(2)==held)return null;
                    aggregateVersion=r.getLong(1)+1;
                }
            }
            if(held)try(var s=c.prepareStatement("SELECT 1 FROM dzoka.media_assets WHERE tenant_id=? AND report_id=? AND purge_started_at IS NOT NULL")){
                s.setObject(1,tenant);s.setObject(2,report);try(var r=s.executeQuery()){if(r.next())throw new Failures.Conflict();}
            }
            try(var s=c.prepareStatement("UPDATE dzoka.reports SET legal_hold=?,version=version+1 WHERE tenant_id=? AND id=?")){
                s.setBoolean(1,held);s.setObject(2,tenant);s.setObject(3,report);s.executeUpdate();
            }
            MutationJournal.append(c,tenant,actor,held?"report.held":"report.hold-cleared",report,aggregateVersion,"report_id");
            return null;
        });
    }
    public static void tombstone(Connection c,UUID tenant,UUID report) throws SQLException {
        try(var s=c.prepareStatement("UPDATE dzoka.reports SET deleted_at=clock_timestamp(),version=version+1 WHERE tenant_id=? AND id=? AND NOT legal_hold AND deleted_at IS NULL")){
            s.setObject(1,tenant);s.setObject(2,report);s.executeUpdate();
        }
    }
    @Override public int expire(VerifiedIdentity actor,UUID tenant){
        return tx.execute(actor,tenant,"reports:purge",c->{
            var ids=new ArrayList<UUID>();
            try(var s=c.prepareStatement("SELECT id FROM dzoka.reports WHERE tenant_id=? AND erased_at IS NULL AND NOT legal_hold AND (expires_at<=clock_timestamp() OR deleted_at IS NOT NULL) ORDER BY id LIMIT 100 FOR UPDATE SKIP LOCKED")){
                s.setObject(1,tenant);try(var r=s.executeQuery()){while(r.next())ids.add(r.getObject(1,UUID.class));}
            }
            for(UUID id:ids){
                tombstone(c,tenant,id);
                try(var s=c.prepareStatement("DELETE FROM dzoka.report_revisions WHERE tenant_id=? AND report_id=?")){s.setObject(1,tenant);s.setObject(2,id);s.executeUpdate();}
                try(var s=c.prepareStatement("UPDATE dzoka.reports SET erased_at=clock_timestamp(),version=version+1 WHERE tenant_id=? AND id=? RETURNING version")){
                    s.setObject(1,tenant);s.setObject(2,id);try(var r=s.executeQuery()){r.next();MutationJournal.append(c,tenant,actor,"report.erased",id,r.getLong(1),"report_id");}
                }
                try(var s=c.prepareStatement("UPDATE dzoka.intake_commands SET request_hash='erased' WHERE tenant_id=? AND report_id=?")){s.setObject(1,tenant);s.setObject(2,id);s.executeUpdate();}
                try(var s=c.prepareStatement("UPDATE dzoka.recovery_cases SET state='ERASED' WHERE tenant_id=? AND id=(SELECT case_id FROM dzoka.reports WHERE tenant_id=? AND id=?)")){
                    s.setObject(1,tenant);s.setObject(2,tenant);s.setObject(3,id);s.executeUpdate();
                }
            }
            return ids.size();
        });
    }
    private static Timestamp timestamp(Instant i){return i==null?null:Timestamp.from(i);}
    private static Instant instant(Timestamp t){return t==null?null:t.toInstant();}
}
