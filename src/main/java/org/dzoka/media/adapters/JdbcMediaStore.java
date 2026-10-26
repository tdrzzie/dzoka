package org.dzoka.media.adapters;

import java.sql.*;
import java.util.*;
import org.dzoka.foundation.application.*;
import org.dzoka.foundation.persistence.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.media.ports.MediaStore;
import org.springframework.stereotype.Component;

@Component
public final class JdbcMediaStore implements MediaStore {
    public static final long RESERVATION=25L*1024*1024, TENANT_LIMIT=500L*1024*1024;
    private final TenantTransactions tx;
    public JdbcMediaStore(TenantTransactions tx){this.tx=tx;}
    private static void report(Connection c,VerifiedIdentity actor,UUID tenant,UUID id,boolean write,boolean lock) throws SQLException {
        try(var s=c.prepareStatement("SELECT owner_ref FROM dzoka.reports WHERE tenant_id=? AND id=? AND deleted_at IS NULL AND expires_at>clock_timestamp()"+(lock?" FOR UPDATE":""))){
            s.setObject(1,tenant);s.setObject(2,id);try(var r=s.executeQuery()){
                if(!r.next())throw new Failures.NotFound();
                if(write && !actor.reference().equals(r.getString(1)))
                    try(var p=c.createStatement();var result=p.executeQuery("SELECT dzoka.permitted('reports:write:any')")){
                        result.next();if(!result.getBoolean(1))throw new Failures.Forbidden();
                    }
            }
        }
    }
    @Override public void authorizeUpload(VerifiedIdentity actor,UUID tenant,UUID id){tx.execute(actor,tenant,"reports:write",c->{report(c,actor,tenant,id,true,false);return null;});}
    @Override public Asset prepare(VerifiedIdentity actor,UUID tenant,UUID report,String key,String sha,int size,String type){
        return tx.execute(actor,tenant,"reports:write",c->{
            report(c,actor,tenant,report,true,true);
            try(var s=c.prepareStatement("SELECT * FROM dzoka.media_assets WHERE tenant_id=? AND report_id=? AND owner_ref=? AND upload_key_hash=?")){
                s.setObject(1,tenant);s.setObject(2,report);s.setString(3,actor.reference());s.setString(4,VerifiedIdentity.digest(key));
                try(var r=s.executeQuery()){if(r.next()){
                    Asset prior=asset(r);if(!prior.sourceSha().equals(sha) || prior.sourceBytes()!=size || !prior.declaredType().equals(type))throw new Failures.Conflict();return prior;
                }}
            }
            try(var s=c.prepareStatement("SELECT count(*) FROM dzoka.media_assets WHERE tenant_id=? AND report_id=? AND NOT reservation_released")){
                s.setObject(1,tenant);s.setObject(2,report);try(var r=s.executeQuery()){r.next();if(r.getInt(1)>=10)throw new Failures.Conflict();}
            }
            try(var s=c.prepareStatement("INSERT INTO dzoka.media_usage(tenant_id) VALUES(?) ON CONFLICT DO NOTHING")){s.setObject(1,tenant);s.executeUpdate();}
            try(var s=c.prepareStatement("UPDATE dzoka.media_usage SET reserved_bytes=reserved_bytes+? WHERE tenant_id=? AND reserved_bytes+?<=(SELECT media_byte_limit FROM dzoka.tenants WHERE id=?)")){
                s.setLong(1,RESERVATION);s.setObject(2,tenant);s.setLong(3,RESERVATION);s.setObject(4,tenant);if(s.executeUpdate()!=1)throw new Failures.Conflict();
            }
            UUID id=UUID.randomUUID();String prefix=tenant+"/"+id+"/";
            try(var s=c.prepareStatement("""
                INSERT INTO dzoka.media_assets(tenant_id,id,report_id,owner_ref,upload_key_hash,source_sha256,source_bytes,
                declared_type,source_key,derivative_key,state,expires_at)
                SELECT ?,?,?,?,?,?,?,?,?,?,'UPLOADING',expires_at FROM dzoka.reports WHERE tenant_id=? AND id=? RETURNING *
                """)){
                s.setObject(1,tenant);s.setObject(2,id);s.setObject(3,report);s.setString(4,actor.reference());s.setString(5,VerifiedIdentity.digest(key));
                s.setString(6,sha);s.setInt(7,size);s.setString(8,type);s.setString(9,prefix+"source");s.setString(10,prefix+UUID.randomUUID());
                s.setObject(11,tenant);s.setObject(12,report);try(var r=s.executeQuery()){r.next();return asset(r);}
            }
        });
    }
    @Override public Asset uploaded(VerifiedIdentity actor,Asset original){
        return tx.execute(actor,original.tenant(),"reports:write",c->{
            try(var s=c.prepareStatement("""
                UPDATE dzoka.media_assets m SET state='QUARANTINED',retry_budget_used=0,rejection_code=NULL WHERE m.tenant_id=? AND m.id=? AND m.state IN ('UPLOADING','FAILED')
                AND EXISTS(SELECT 1 FROM dzoka.reports r WHERE r.tenant_id=m.tenant_id AND r.id=m.report_id
                  AND r.deleted_at IS NULL AND r.expires_at>clock_timestamp()) RETURNING m.*
                """)){
                s.setObject(1,original.tenant());s.setObject(2,original.id());try(var r=s.executeQuery()){if(r.next()){
                    Asset result=asset(r);
                    try(var existing=c.prepareStatement("SELECT 1 FROM dzoka.outbox WHERE tenant_id=? AND media_id=? AND event_type='media.quarantined'")){
                        existing.setObject(1,result.tenant());existing.setObject(2,result.id());try(var rows=existing.executeQuery()){
                            if(!rows.next())MutationJournal.append(c,result.tenant(),actor,"media.quarantined",result.id(),1,"media_id");
                        }
                    }return result;
                }}
            }
            Asset now=lookup(c,original.tenant(),original.id());
            if(parentInactive(c,now)){queueCleanup(c,now);return null;}
            return now;
        });
    }
    @Override public Asset read(VerifiedIdentity actor,UUID tenant,UUID id){
        return tx.execute(actor,tenant,"reports:read",c->{Asset a=lookup(c,tenant,id);report(c,actor,tenant,a.report(),false,false);return a;});
    }
    @Override public Asset retry(VerifiedIdentity actor,UUID tenant,UUID id){
        return tx.execute(actor,tenant,"reports:write",c->{
            Asset a=lookup(c,tenant,id);report(c,actor,tenant,a.report(),true,true);
            try(var s=c.prepareStatement("UPDATE dzoka.media_assets SET state='QUARANTINED',retry_budget_used=0,rejection_code=NULL WHERE tenant_id=? AND id=? AND state='FAILED' RETURNING *")){
                s.setObject(1,tenant);s.setObject(2,id);try(var r=s.executeQuery()){if(r.next())return asset(r);}
            }
            Asset current=lookup(c,tenant,id);if(!current.state().equals("QUARANTINED"))throw new Failures.Conflict();return current;
        });
    }
    private static Asset lookup(Connection c,UUID tenant,UUID id) throws SQLException {
        try(var s=c.prepareStatement("SELECT * FROM dzoka.media_assets WHERE tenant_id=? AND id=?")){
            s.setObject(1,tenant);s.setObject(2,id);try(var r=s.executeQuery()){if(!r.next())throw new Failures.NotFound();return asset(r);}
        }
    }
    private static boolean parentInactive(Connection c,Asset a) throws SQLException {
        try(var s=c.prepareStatement("SELECT 1 FROM dzoka.reports WHERE tenant_id=? AND id=? AND deleted_at IS NULL AND expires_at>clock_timestamp()")){
            s.setObject(1,a.tenant());s.setObject(2,a.report());try(var r=s.executeQuery()){return !r.next();}
        }
    }
    private static void queueCleanup(Connection c,Asset a) throws SQLException {
        try(var s=c.prepareStatement("UPDATE dzoka.media_assets SET state='DELETED',purge_pending=true,purge_version=purge_version+1,lease_token=NULL,lease_owner=NULL,lease_until=NULL WHERE tenant_id=? AND id=?")){
            s.setObject(1,a.tenant());s.setObject(2,a.id());s.executeUpdate();
        }
    }
    @Override public Optional<Asset> claim(VerifiedIdentity worker,UUID tenant){
        return tx.execute(worker,tenant,"media:process",c->{
            try(var s=c.prepareStatement("UPDATE dzoka.media_assets SET state='FAILED',rejection_code='ATTEMPTS_EXHAUSTED',lease_token=NULL,lease_owner=NULL,lease_until=NULL WHERE tenant_id=? AND state='PROCESSING' AND retry_budget_used>=3 AND lease_until<clock_timestamp()")){
                s.setObject(1,tenant);s.executeUpdate();
            }
            try(var s=c.prepareStatement("""
                SELECT m.* FROM dzoka.media_assets m JOIN dzoka.reports r ON r.tenant_id=m.tenant_id AND r.id=m.report_id
                WHERE m.tenant_id=? AND r.deleted_at IS NULL AND r.expires_at>clock_timestamp() AND m.retry_budget_used<3 AND (
                  m.state='QUARANTINED' OR (m.state='PROCESSING' AND m.lease_until<clock_timestamp())
                  OR (m.state='UPLOADING' AND m.created_at<clock_timestamp()-interval '30 seconds'))
                ORDER BY m.created_at,m.id LIMIT 1 FOR UPDATE OF m SKIP LOCKED
                """)){
                s.setObject(1,tenant);try(var r=s.executeQuery()){
                    if(!r.next())return Optional.empty();UUID id=r.getObject("id",UUID.class);
                    try(var u=c.prepareStatement("UPDATE dzoka.media_assets SET state='PROCESSING',lease_token=?,lease_owner=?,lease_until=clock_timestamp()+interval '120 seconds',attempts=attempts+1,retry_budget_used=retry_budget_used+1 WHERE tenant_id=? AND id=? RETURNING *")){
                        u.setObject(1,UUID.randomUUID());u.setString(2,worker.reference());u.setObject(3,tenant);u.setObject(4,id);
                        try(var result=u.executeQuery()){result.next();return Optional.of(asset(result));}
                    }
                }
            }
        });
    }
    @Override public boolean complete(VerifiedIdentity worker,Asset a,String sha,int bytes,int width,int height,String policy){
        return tx.execute(worker,a.tenant(),"media:process",c->{
            if(parentInactive(c,a)){queueCleanup(c,a);return false;}
            try(var s=c.prepareStatement("""
                UPDATE dzoka.media_assets SET state='SANITIZED',derivative_sha256=?,derivative_bytes=?,width=?,height=?,
                sanitizer_version=?,lease_token=NULL,lease_owner=NULL,lease_until=NULL
                WHERE tenant_id=? AND id=? AND state='PROCESSING' AND lease_token=? AND lease_owner=?
                  AND lease_until>clock_timestamp() RETURNING attempts
                """)){
                s.setString(1,sha);s.setInt(2,bytes);s.setInt(3,width);s.setInt(4,height);s.setString(5,policy);
                s.setObject(6,a.tenant());s.setObject(7,a.id());s.setObject(8,a.lease());s.setString(9,worker.reference());
                try(var r=s.executeQuery()){if(!r.next())return false;
                    MutationJournal.append(c,a.tenant(),worker,"media.sanitized",a.id(),2,"media_id");return true;
                }
            }
        });
    }
    @Override public void fail(VerifiedIdentity worker,Asset a,String reason,boolean permanent){
        tx.execute(worker,a.tenant(),"media:process",c->{
            try(var s=c.prepareStatement("""
                UPDATE dzoka.media_assets SET state=CASE WHEN ? THEN 'REJECTED' WHEN retry_budget_used>=3 THEN 'FAILED' ELSE 'QUARANTINED' END,
                rejection_code=?,purge_pending=?,purge_version=purge_version+1,
                lease_token=NULL,lease_owner=NULL,lease_until=NULL WHERE tenant_id=? AND id=? AND lease_token=? AND lease_owner=? AND lease_until>clock_timestamp()
                """)){
                s.setBoolean(1,permanent);s.setString(2,reason);s.setBoolean(3,permanent);s.setObject(4,a.tenant());s.setObject(5,a.id());
                s.setObject(6,a.lease());s.setString(7,worker.reference());s.executeUpdate();
            }return null;
        });
    }
    @Override public Optional<Asset> purgeCandidate(VerifiedIdentity worker,UUID tenant){
        return tx.execute(worker,tenant,"media:purge",c->{
            try(var s=c.createStatement();var r=s.executeQuery("SELECT dzoka.permitted('reports:purge')")){
                r.next();if(!r.getBoolean(1))throw new Failures.Forbidden();
            }
            UUID report;boolean parentDeleted;
            try(var s=c.prepareStatement("""
                SELECT r.id,r.deleted_at IS NOT NULL FROM dzoka.reports r WHERE r.tenant_id=? AND NOT r.legal_hold AND EXISTS(
                    SELECT 1 FROM dzoka.media_assets m WHERE m.tenant_id=r.tenant_id AND m.report_id=r.id
                    AND (m.purge_pending OR (r.deleted_at IS NOT NULL AND NOT m.reservation_released)))
                ORDER BY r.id LIMIT 1 FOR UPDATE OF r SKIP LOCKED
                """)){
                s.setObject(1,tenant);try(var r=s.executeQuery()){if(!r.next())return Optional.empty();report=r.getObject(1,UUID.class);parentDeleted=r.getBoolean(2);}
            }
            try(var s=c.prepareStatement("""
                UPDATE dzoka.media_assets SET purge_started_at=coalesce(purge_started_at,clock_timestamp()),purge_pending=true,
                state=CASE WHEN state='REJECTED' THEN 'REJECTED' ELSE 'DELETED' END,
                lease_token=NULL,lease_owner=NULL,lease_until=NULL
                WHERE tenant_id=? AND id=(SELECT id FROM dzoka.media_assets WHERE tenant_id=? AND report_id=?
                    AND (purge_pending OR (? AND NOT reservation_released)) ORDER BY created_at,id LIMIT 1 FOR UPDATE SKIP LOCKED) RETURNING *
                """)){
                s.setObject(1,tenant);s.setObject(2,tenant);s.setObject(3,report);s.setBoolean(4,parentDeleted);
                try(var r=s.executeQuery()){return r.next()?Optional.of(asset(r)):Optional.empty();}
            }
        });
    }
    @Override public boolean purged(VerifiedIdentity worker,Asset a){
        return tx.execute(worker,a.tenant(),"media:purge",c->{
            try(var s=c.prepareStatement("SELECT * FROM dzoka.media_assets WHERE tenant_id=? AND id=? FOR UPDATE")){
                s.setObject(1,a.tenant());s.setObject(2,a.id());try(var r=s.executeQuery()){
                    if(!r.next() || r.getLong("purge_version")!=a.purgeVersion())return false;
                    boolean released=r.getBoolean("reservation_released");
                    try(var u=c.prepareStatement("UPDATE dzoka.media_assets SET purge_pending=false,reservation_released=true WHERE tenant_id=? AND id=?")){
                        u.setObject(1,a.tenant());u.setObject(2,a.id());u.executeUpdate();
                    }
                    if(!released)try(var u=c.prepareStatement("UPDATE dzoka.media_usage SET reserved_bytes=reserved_bytes-? WHERE tenant_id=?")){
                        u.setLong(1,RESERVATION);u.setObject(2,a.tenant());u.executeUpdate();
                    }
                    return true;
                }
            }
        });
    }
    private static Asset asset(ResultSet r) throws SQLException {
        return new Asset(r.getObject("tenant_id",UUID.class),r.getObject("id",UUID.class),r.getObject("report_id",UUID.class),r.getString("state"),
            r.getString("source_key"),r.getString("source_sha256"),r.getLong("source_bytes"),r.getString("declared_type"),r.getString("derivative_key"),
            r.getString("derivative_sha256"),r.getLong("derivative_bytes"),(Integer)r.getObject("width"),(Integer)r.getObject("height"),
            r.getTimestamp("expires_at").toInstant(),r.getString("rejection_code"),r.getObject("lease_token",UUID.class),r.getLong("purge_version"));
    }
}
