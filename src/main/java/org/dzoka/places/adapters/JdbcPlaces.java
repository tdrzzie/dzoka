package org.dzoka.places.adapters;
import java.sql.*;
import java.util.*;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.places.domain.*;
import org.dzoka.places.ports.PlaceCatalog;
import org.springframework.stereotype.Component;
@Component
public final class JdbcPlaces implements PlaceCatalog {
    private final TenantTransactions tx;
    public JdbcPlaces(TenantTransactions tx){this.tx=tx;}
    private static void lock(Connection c,UUID tenant,boolean shared)throws SQLException{
        try(var s=c.prepareStatement("SELECT "+(shared?"pg_advisory_xact_lock_shared":"pg_advisory_xact_lock")+"(hashtextextended(?,67))")){s.setString(1,"places:"+tenant);s.execute();}
    }
    private static long current(Connection c,UUID tenant,UUID id)throws SQLException{
        try(var s=c.prepareStatement("SELECT version FROM dzoka.places WHERE tenant_id=? AND id=?")){s.setObject(1,tenant);s.setObject(2,id);try(var r=s.executeQuery()){if(!r.next())throw new Failures.NotFound();return r.getLong(1);}}
    }
    private static PlaceView read(Connection c,UUID tenant,UUID id,long version)throws SQLException{
        try(var s=c.prepareStatement("SELECT * FROM dzoka.place_revisions WHERE tenant_id=? AND place_id=? AND version=?")){
            s.setObject(1,tenant);s.setObject(2,id);s.setLong(3,version);
            try(var r=s.executeQuery()){
                if(!r.next())throw new Failures.NotFound();var aliases=new ArrayList<String>();
                try(var a=c.prepareStatement("SELECT raw_label FROM dzoka.place_aliases WHERE tenant_id=? AND place_id=? AND version=? ORDER BY ordinal")){a.setObject(1,tenant);a.setObject(2,id);a.setLong(3,version);try(var ar=a.executeQuery()){while(ar.next())aliases.add(ar.getString(1));}}
                return new PlaceView(id,version,r.getString("name"),r.getString("kind"),r.getObject("parent_id",UUID.class),(Long)r.getObject("parent_version"),r.getBoolean("active"),(Double)r.getObject("latitude"),(Double)r.getObject("longitude"),(Double)r.getObject("uncertainty_meters"),r.getString("timezone"),List.copyOf(aliases));
            }
        }
    }
    private static int path(Connection c,UUID tenant,UUID parent,UUID forbidden,boolean mustBeActive)throws SQLException{
        var visited=new HashSet<UUID>();int depth=0;
        while(parent!=null){
            if(parent.equals(forbidden)||!visited.add(parent)||++depth>32)throw new Failures.Conflict();
            try(var s=c.prepareStatement("SELECT parent_id,active FROM dzoka.places WHERE tenant_id=? AND id=?")){s.setObject(1,tenant);s.setObject(2,parent);try(var r=s.executeQuery()){if(!r.next())throw new Failures.NotFound();if(mustBeActive&&!r.getBoolean(2))throw new Failures.Conflict();parent=r.getObject(1,UUID.class);}}
        }return depth;
    }
    @Override public void requireReference(Connection c,UUID tenant,UUID id,Long version)throws SQLException{
        if(id==null){if(version!=null)throw new IllegalArgumentException();return;}
        lock(c,tenant,true);
        if(version==null||current(c,tenant,id)!=version)throw new Failures.Conflict();
        path(c,tenant,id,null,true);
    }
    @Override public PlaceView save(VerifiedIdentity actor,UUID tenant,UUID target,PlaceDraft d,String key){
        String digest=PlaceCommandHash.canonical(target,d),keyHash=VerifiedIdentity.digest(key);
        return tx.execute(actor,tenant,"places:manage",c->{
            lock(c,tenant,false);
            try(var s=c.prepareStatement("SELECT request_hash,place_id,version,request_hash_version FROM dzoka.place_commands WHERE tenant_id=? AND actor_ref=? AND key_hash=?")){
                s.setObject(1,tenant);s.setString(2,actor.reference());s.setString(3,keyHash);try(var r=s.executeQuery()){if(r.next()){if(!PlaceCommandHash.matches(r.getInt(4),r.getString(1),target,d))throw new Failures.Conflict();return read(c,tenant,r.getObject(2,UUID.class),r.getLong(3));}}
            }
            UUID id=target==null?UUID.randomUUID():target;long next=target==null?1:current(c,tenant,id)+1;
            if(next!=d.expectedVersion()+1)throw new Failures.Conflict();
            int parentDepth=path(c,tenant,d.parentId(),id,d.active());
            int subtreeDepth=1;
            if(target!=null){
                try(var s=c.prepareStatement("WITH RECURSIVE subtree(id,depth) AS(SELECT id,1 FROM dzoka.places WHERE tenant_id=? AND id=? UNION ALL SELECT p.id,s.depth+1 FROM dzoka.places p JOIN subtree s ON p.parent_id=s.id WHERE p.tenant_id=? AND s.depth<=32) SELECT max(depth) FROM subtree")){s.setObject(1,tenant);s.setObject(2,id);s.setObject(3,tenant);try(var r=s.executeQuery()){r.next();subtreeDepth=r.getInt(1);}}
                if(!d.active())try(var s=c.prepareStatement("SELECT 1 FROM dzoka.places WHERE tenant_id=? AND parent_id=? AND active LIMIT 1")){s.setObject(1,tenant);s.setObject(2,id);try(var r=s.executeQuery()){if(r.next())throw new Failures.Conflict();}}
            }
            if(parentDepth+subtreeDepth>32)throw new Failures.Conflict();
            Long parentVersion=d.parentId()==null?null:current(c,tenant,d.parentId());
            try(var s=c.prepareStatement("INSERT INTO dzoka.places(tenant_id,id,version,parent_id,active) VALUES(?,?,?,?,?) ON CONFLICT(tenant_id,id) DO UPDATE SET version=excluded.version,parent_id=excluded.parent_id,active=excluded.active")){s.setObject(1,tenant);s.setObject(2,id);s.setLong(3,next);s.setObject(4,d.parentId());s.setBoolean(5,d.active());s.executeUpdate();}
            try(var s=c.prepareStatement("INSERT INTO dzoka.place_revisions(tenant_id,place_id,version,name,normalized_name,kind,parent_id,parent_version,active,latitude,longitude,uncertainty_meters,timezone,actor_ref) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)")){
                s.setObject(1,tenant);s.setObject(2,id);s.setLong(3,next);s.setString(4,d.name());s.setString(5,PlaceDraft.normalize(d.name()));s.setString(6,d.kind());s.setObject(7,d.parentId());s.setObject(8,parentVersion);s.setBoolean(9,d.active());s.setObject(10,d.latitude());s.setObject(11,d.longitude());s.setObject(12,d.uncertaintyMeters());s.setString(13,d.timezone());s.setString(14,actor.reference());s.executeUpdate();
            }
            try(var s=c.prepareStatement("INSERT INTO dzoka.place_aliases(tenant_id,place_id,version,ordinal,raw_label,normalized_label) VALUES(?,?,?,?,?,?)")){int ordinal=0;for(String alias:d.aliases()){s.setObject(1,tenant);s.setObject(2,id);s.setLong(3,next);s.setInt(4,ordinal++);s.setString(5,alias);s.setString(6,PlaceDraft.normalize(alias));s.addBatch();}s.executeBatch();}
            try(var s=c.prepareStatement("INSERT INTO dzoka.place_commands(tenant_id,actor_ref,key_hash,request_hash,place_id,version,request_hash_version) VALUES(?,?,?,?,?,?,?)")){s.setObject(1,tenant);s.setString(2,actor.reference());s.setString(3,keyHash);s.setString(4,digest);s.setObject(5,id);s.setLong(6,next);s.setInt(7,PlaceCommandHash.CURRENT_VERSION);s.executeUpdate();}
            String event=target==null?"place.created":"place.updated";
            try(var s=c.prepareStatement("INSERT INTO dzoka.audit_events(tenant_id,id,actor_ref,action,entity_id) VALUES(?,?,?,?,?)")){s.setObject(1,tenant);s.setObject(2,UUID.randomUUID());s.setString(3,actor.reference());s.setString(4,event);s.setObject(5,id);s.executeUpdate();}
            try(var s=c.prepareStatement("INSERT INTO dzoka.outbox(tenant_id,id,event_type,aggregate_id,aggregate_version,place_id,payload) VALUES(?,?,?,?,?,?,jsonb_build_object('placeId',CAST(? AS text)))")){s.setObject(1,tenant);s.setObject(2,UUID.randomUUID());s.setString(3,event);s.setObject(4,id);s.setLong(5,next);s.setObject(6,id);s.setString(7,id.toString());s.executeUpdate();}
            return read(c,tenant,id,next);
        });
    }
    @Override public PlaceView get(VerifiedIdentity actor,UUID tenant,UUID id,Long version){return tx.execute(actor,tenant,"places:read",c->read(c,tenant,id,version==null?current(c,tenant,id):version));}
    @Override public Page page(VerifiedIdentity actor,UUID tenant,UUID after,int limit,String label,boolean includeInactive){return tx.execute(actor,tenant,"places:read",c->{
        var result=new ArrayList<PlaceView>();
        try(var s=c.prepareStatement("SELECT p.id,p.version FROM dzoka.places p JOIN dzoka.place_revisions v ON(v.tenant_id,v.place_id,v.version)=(p.tenant_id,p.id,p.version) WHERE p.tenant_id=? AND (? OR p.active) AND (?::uuid IS NULL OR p.id>?) AND (?::text IS NULL OR v.normalized_name=? OR EXISTS(SELECT 1 FROM dzoka.place_aliases a WHERE (a.tenant_id,a.place_id,a.version)=(p.tenant_id,p.id,p.version) AND a.normalized_label=?)) ORDER BY p.id LIMIT ?")){
            s.setObject(1,tenant);s.setBoolean(2,includeInactive);s.setObject(3,after);s.setObject(4,after);s.setString(5,label);s.setString(6,label);s.setString(7,label);s.setInt(8,limit+1);try(var r=s.executeQuery()){while(r.next())result.add(read(c,tenant,r.getObject(1,UUID.class),r.getLong(2)));}
        }boolean more=result.size()>limit;if(more)result.removeLast();return new Page(List.copyOf(result),more?result.getLast().id():null);
    });}
    @Override public List<PlaceView> ancestors(VerifiedIdentity actor,UUID tenant,UUID id,Long version){return tx.execute(actor,tenant,"places:read",c->{
        lock(c,tenant,true);var result=new ArrayList<PlaceView>();var seen=new HashSet<String>();Long revision=version;UUID cursor=id;
        while(cursor!=null){long selected=revision==null?current(c,tenant,cursor):revision;if(result.size()>=32||!seen.add(cursor+":"+selected))throw new Failures.Conflict();var p=read(c,tenant,cursor,selected);result.add(p);cursor=p.parentId();revision=version==null?null:p.parentVersion();}
        return List.copyOf(result);
    });}
}
