package org.dzoka.federation;

import static org.dzoka.federation.FederationWire.*;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.dzoka.foundation.http.BoundedPeerHttp;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public final class Federation {
    public static final String PEER_ROUTE="/api/v1/federation/peer";
    private static final String ADMIN="federation:admin", SERVICE_PERMISSION="federation:service";
    private static final VerifiedIdentity SERVICE=new VerifiedIdentity("urn:dzoka:federation","node");
    private static final Set<String> COUNTRIES=Set.of(java.util.Locale.getISOCountries());
    private static final int PEER_LIMIT=4, BUDGET=30, LIMIT=10;
    private static final String POLICY="coarse-published-v1";
    private final TenantTransactions tx;
    private final UUID tenant;
    private final String node,keyId;
    private final PrivateKey privateKey;
    private final Map<String,URI> routes;
    private final Semaphore fanouts=new Semaphore(4);

    public enum Category { BAG, CLOTHING, DOCUMENT, ELECTRONICS, JEWELRY, KEYS, OTHER, SPORTING_GOODS, UMBRELLA, WALLET }
    public record Peer(boolean inbound,boolean outbound,boolean active,int minVersion,int maxVersion) {}
    public record Key(String publicKey) {}
    public record Offer(int revision,Category category,String country,String month,boolean active) {}
    public record Query(Category category,String country,String month,int limit) {
        public Query {
            if(category==null || country==null || !COUNTRIES.contains(country) || limit<1 || limit>LIMIT)
                throw new IllegalArgumentException("Invalid coarse federation search");
            period(month);
        }
    }
    public record Search(List<String> peers,Query query) {}
    public record Candidate(UUID reference,int rank,String freshness) {}
    public record Discovery(String policy,List<Candidate> candidates) {}
    public record Capabilities(List<Integer> versions,List<String> operations,int maxResults,String policy) {}
    public record PeerResult(String peer,String state,List<Candidate> candidates) {}
    public record Hit(String peer,UUID reference,int peerRank,String freshness,String policy,String calibration) {}
    public record Result(boolean partial,List<PeerResult> peers,List<Hit> hits) {}
    public record Health(String peer,boolean active,boolean inbound,boolean outbound,int recentFailures,boolean circuitOpen) {}
    private record Attempt(String peer,UUID request,URI route,String state) {}

    public Federation(TenantTransactions tx,Environment env) {
        this.tx=tx;
        String tenantValue=env.getProperty("DZOKA_FEDERATION_TENANT","");
        tenant=tenantValue.isEmpty()?null:UUID.fromString(tenantValue);
        node=env.getProperty("DZOKA_FEDERATION_NODE","");
        keyId=env.getProperty("DZOKA_FEDERATION_KEY_ID","");
        boolean fixture=env.getProperty("DZOKA_FEDERATION_TEST_LOOPBACK",Boolean.class,false);
        if(tenant!=null) {
            identifier(node);identifier(keyId);
            privateKey=privateKey(env.getProperty("DZOKA_FEDERATION_PRIVATE_KEY"));
        } else privateKey=null;
        var allowed=new HashMap<String,URI>();
        String configured=env.getProperty("DZOKA_FEDERATION_ORIGINS","");
        if(!configured.isBlank()) {
            for(String entry:configured.split(",",-1)) {
                String[] parts=entry.split("=",2);
                identifier(parts[0]);
                if(parts.length!=2 || parts[0].equals(node) || allowed.putIfAbsent(parts[0],BoundedPeerHttp.origin(parts[1],fixture))!=null)
                    throw new IllegalArgumentException("Invalid federation route configuration");
            }
        }
        routes=Map.copyOf(allowed);
    }

    public void peer(VerifiedIdentity actor,UUID scope,String peer,Peer policy) {
        scope(scope);peerName(peer);
        if(policy==null || policy.minVersion()<1 || policy.maxVersion()<policy.minVersion() || policy.maxVersion()>100)
            throw new IllegalArgumentException("Invalid peer version range");
        tx.execute(actor,scope,ADMIN,db -> {
            lock(db,peer);
            write(db,"""
                    INSERT INTO dzoka.federation_peers(tenant_id,peer,inbound,outbound,active,min_version,max_version)
                    VALUES(?,?,?,?,?,?,?) ON CONFLICT(tenant_id,peer) DO UPDATE
                    SET inbound=excluded.inbound,outbound=excluded.outbound,active=excluded.active,
                        min_version=excluded.min_version,max_version=excluded.max_version,failures=0,retry_at=NULL
                    """,scope,peer,policy.inbound(),policy.outbound(),policy.active(),policy.minVersion(),policy.maxVersion());
            return null;
        });
    }

    public void key(VerifiedIdentity actor,UUID scope,String peer,String id,Key key) {
        scope(scope);peerName(peer);identifier(id);
        if(key==null) throw new IllegalArgumentException("Verification key required");
        publicKey(key.publicKey());
        tx.execute(actor,scope,ADMIN,db -> {
            lock(db,peer);
            requirePeer(db,peer);
            write(db,"""
                    INSERT INTO dzoka.federation_keys(tenant_id,peer,key_id,public_key) VALUES(?,?,?,?) ON CONFLICT DO NOTHING
                    """,scope,peer,id,key.publicKey());
            try(var query=q(db,"SELECT public_key,revoked_at FROM dzoka.federation_keys WHERE peer=? AND key_id=?",peer,id);
                var row=query.executeQuery()) {
                if(!row.next() || !key.publicKey().equals(row.getString(1)) || row.getObject(2)!=null)
                    throw new Failures.Conflict();
            }
            return null;
        });
    }

    public void revoke(VerifiedIdentity actor,UUID scope,String peer,String id) {
        scope(scope);peerName(peer);identifier(id);
        tx.execute(actor,scope,ADMIN,db -> {
            lock(db,peer);
            if(write(db,"UPDATE dzoka.federation_keys SET revoked_at=coalesce(revoked_at,clock_timestamp()) WHERE peer=? AND key_id=?",
                    peer,id)==0) throw new Failures.NotFound();
            return null;
        });
    }

    public void offer(VerifiedIdentity actor,UUID scope,UUID report,Offer offer) {
        scope(scope);
        if(offer==null || offer.revision()<1) throw new IllegalArgumentException("Revision required");
        tx.execute(actor,scope,ADMIN,db -> {
            if(!offer.active()) {
                if(write(db,"UPDATE dzoka.federation_offers SET active=false WHERE report_id=?",report)==0) throw new Failures.NotFound();
                return null;
            }
            var query=new Query(offer.category(),offer.country(),offer.month(),1);
            try(var allowed=q(db,"""
                    SELECT 1 FROM dzoka.reports WHERE id=? AND current_revision=? AND kind='FOUND'
                    AND dzoka.permitted('reports:read') AND NOT legal_hold AND deleted_at IS NULL AND expires_at>clock_timestamp()
                    """,report,offer.revision());var row=allowed.executeQuery()) {
                if(!row.next()) throw new Failures.NotFound();
            }
            write(db,"""
                    INSERT INTO dzoka.federation_offers(tenant_id,report_id,revision,category,country,observed_month,active)
                    VALUES(?,?,?,?,?,?,true) ON CONFLICT(tenant_id,report_id) DO UPDATE SET revision=excluded.revision,
                        category=excluded.category,country=excluded.country,observed_month=excluded.observed_month,
                        offer_id=CASE WHEN federation_offers.revision=excluded.revision AND federation_offers.active
                            THEN federation_offers.offer_id ELSE gen_random_uuid() END,
                        published_month=excluded.published_month,active=true
                    """,scope,report,offer.revision(),query.category().name(),query.country(),period(query.month()).atDay(1));
            return null;
        });
    }

    public List<Health> health(VerifiedIdentity actor,UUID scope) {
        scope(scope);
        return tx.execute(actor,scope,ADMIN,db -> {
            var result=new ArrayList<Health>();
            try(var query=q(db,"""
                    SELECT peer,active,inbound,outbound,failures,coalesce(retry_at>clock_timestamp(),false)
                    FROM dzoka.federation_peers ORDER BY peer LIMIT 100
                    """);var rows=query.executeQuery()) {
                while(rows.next()) result.add(new Health(rows.getString(1),rows.getBoolean(2),rows.getBoolean(3),
                        rows.getBoolean(4),rows.getInt(5),rows.getBoolean(6)));
            }
            return List.copyOf(result);
        });
    }

    public byte[] receive(byte[] bytes) {
        requireNode();
        var request=decode(bytes,Envelope.class);
        if(!node.equals(request.audience()) || node.equals(request.issuer()) || !request.operation().endsWith("_REQUEST"))
            throw new Failures.Forbidden();
        return tx.execute(SERVICE,tenant,SERVICE_PERMISSION,db -> {
            lock(db,request.issuer());
            var key=trusted(db,request.issuer(),request.keyId(),true);
            byte[] body=verify(request,key);
            if(!allow(db,request.issuer(),"IN")) throw new Failures.Conflict();
            if(write(db,"""
                    INSERT INTO dzoka.federation_traffic(tenant_id,peer,request_id,direction,operation,outcome)
                    VALUES(?,?,?,'IN',?,'ACCEPTED') ON CONFLICT DO NOTHING
                    """,tenant,request.issuer(),request.request(),request.operation())==0) throw new Failures.Conflict();
            Object response;
            if("SEARCH_REQUEST".equals(request.operation())) response=discover(db,request.issuer(),decode(body,Query.class));
            else {
                if(!decode(body,Map.class).isEmpty()) throw new IllegalArgumentException("Invalid capability request");
                response=capabilities();
            }
            return encode(sign(request.operation().replace("_REQUEST","_RESPONSE"),node,request.issuer(),keyId,
                    request.request(),response,privateKey));
        });
    }

    public Result search(VerifiedIdentity actor,UUID scope,Search search) {
        scope(scope);
        tx.execute(actor,scope,"federation:search",db -> null);
        if(search==null || search.query()==null || search.peers()==null || search.peers().isEmpty() || search.peers().size()>PEER_LIMIT)
            throw new IllegalArgumentException("Bounded peer selection and query required");
        search.peers().forEach(this::peerName);
        if(new HashSet<>(search.peers()).size()!=search.peers().size()) throw new IllegalArgumentException("Duplicate peer");
        if(!fanouts.tryAcquire()) throw new Failures.Conflict();
        try {
            var tasks=new ArrayList<Callable<PeerResult>>();
            for(String peer:search.peers()) tasks.add(() -> searchPeer(peer,search.query()));
            var results=new ArrayList<PeerResult>();
            var executor=Executors.newVirtualThreadPerTaskExecutor();
            try {
                var futures=executor.invokeAll(tasks,5,TimeUnit.SECONDS);
                for(int i=0;i<futures.size();i++) {
                    var future=futures.get(i);
                    String peer=search.peers().get(i);
                    try { results.add(future.get()); }
                    catch(java.util.concurrent.CancellationException timeout) { results.add(empty(peer,"TIMEOUT")); }
                    catch(java.util.concurrent.ExecutionException failed) { results.add(empty(peer,"UNAVAILABLE")); }
                }
            } catch(InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new Failures.Unavailable(new IllegalStateException("Federation search interrupted"));
            } finally { executor.shutdownNow(); }
            var hits=new ArrayList<Hit>();
            for(int rank=0;rank<search.query().limit();rank++) for(var result:results)
                if(rank<result.candidates().size()) {
                    var candidate=result.candidates().get(rank);
                    hits.add(new Hit(result.peer(),candidate.reference(),candidate.rank(),candidate.freshness(),POLICY,"UNCALIBRATED"));
                }
            return new Result(results.stream().anyMatch(result -> !result.state().equals("COMPLETE")),List.copyOf(results),List.copyOf(hits));
        } finally { fanouts.release(); }
    }

    public Capabilities negotiate(VerifiedIdentity actor,UUID scope,String peer) {
        scope(scope);peerName(peer);
        tx.execute(actor,scope,"federation:search",db -> null);
        var attempt=begin(peer,"CAPABILITIES_REQUEST");
        if(!"READY".equals(attempt.state())) throw new Failures.Conflict();
        try {
            var result=exchange(attempt,"CAPABILITIES",Map.of(),Capabilities.class);
            if(!capabilities().equals(result)) throw new Failures.Conflict();
            finish(attempt,"COMPLETE");
            return result;
        } catch(IOException | RuntimeException failure) {
            finish(attempt,"FAILED");
            throw new Failures.Unavailable(new IllegalStateException("Peer capability exchange failed"));
        }
    }

    private PeerResult searchPeer(String peer,Query query) {
        var attempt=begin(peer,"SEARCH_REQUEST");
        if(!"READY".equals(attempt.state())) return empty(peer,attempt.state());
        try {
            var reply=exchange(attempt,"SEARCH",query,Discovery.class);
            if(!POLICY.equals(reply.policy()) || reply.candidates()==null || reply.candidates().size()>query.limit())
                throw new IllegalArgumentException("Invalid peer discovery");
            var seen=new HashSet<UUID>();
            int rank=0;
            for(var candidate:reply.candidates()) {
                if(candidate==null || candidate.reference()==null || !seen.add(candidate.reference()) || candidate.rank()!=++rank)
                    throw new IllegalArgumentException("Invalid peer rank");
                period(candidate.freshness());
            }
            finish(attempt,"COMPLETE");
            return new PeerResult(peer,"COMPLETE",List.copyOf(reply.candidates()));
        } catch(java.net.SocketTimeoutException timeout) {
            finish(attempt,"TIMEOUT");
            return empty(peer,"TIMEOUT");
        } catch(Failures.Forbidden | Failures.Conflict rejected) {
            finish(attempt,"REFUSED");
            return empty(peer,"REFUSED");
        } catch(IOException | RuntimeException failure) {
            finish(attempt,"FAILED");
            return empty(peer,"UNAVAILABLE");
        }
    }

    private <T> T exchange(Attempt attempt,String operation,Object body,Class<T> type) throws IOException {
        var request=sign(operation+"_REQUEST",node,attempt.peer(),keyId,attempt.request(),body,privateKey);
        var response=BoundedPeerHttp.exchange(attempt.route(),PEER_ROUTE,encode(request),Map.of("Content-Type","application/json"),MAX_BYTES);
        if(response.status()==403 || response.status()==409 || response.status()==429) throw new Failures.Forbidden();
        if(response.status()!=200) throw new IOException("Peer unavailable");
        var envelope=decode(response.body(),Envelope.class);
        if(!node.equals(envelope.audience()) || !attempt.peer().equals(envelope.issuer()) || !attempt.request().equals(envelope.request())
                || !(operation+"_RESPONSE").equals(envelope.operation())) throw new Failures.Forbidden();
        return tx.execute(SERVICE,tenant,SERVICE_PERMISSION,db -> {
            lock(db,attempt.peer());
            return decode(verify(envelope,trusted(db,attempt.peer(),envelope.keyId(),false)),type);
        });
    }

    private Attempt begin(String peer,String operation) {
        UUID id=UUID.randomUUID();
        URI route=routes.get(peer);
        if(route==null) return new Attempt(peer,id,null,"UNROUTABLE");
        return tx.execute(SERVICE,tenant,SERVICE_PERMISSION,db -> {
            lock(db,peer);
            try(var query=q(db,"""
                    SELECT active AND outbound AND min_version<=? AND max_version>=?,coalesce(retry_at>clock_timestamp(),false)
                    FROM dzoka.federation_peers WHERE peer=?
                    """,VERSION,VERSION,peer);var row=query.executeQuery()) {
                if(!row.next() || !row.getBoolean(1)) return new Attempt(peer,id,route,"REFUSED");
                if(row.getBoolean(2)) return new Attempt(peer,id,route,"CIRCUIT_OPEN");
            }
            if(!allow(db,peer,"OUT")) return new Attempt(peer,id,route,"LIMITED");
            write(db,"""
                    INSERT INTO dzoka.federation_traffic(tenant_id,peer,request_id,direction,operation,outcome)
                    VALUES(?,?,?,'OUT',?,'STARTED')
                    """,tenant,peer,id,operation);
            return new Attempt(peer,id,route,"READY");
        });
    }

    private void finish(Attempt attempt,String state) {
        tx.execute(SERVICE,tenant,SERVICE_PERMISSION,db -> {
            lock(db,attempt.peer());
            write(db,"""
                    UPDATE dzoka.federation_traffic SET outcome=? WHERE peer=? AND request_id=? AND direction='OUT'
                    """,state,attempt.peer(),attempt.request());
            if("COMPLETE".equals(state)) write(db,"UPDATE dzoka.federation_peers SET failures=0,retry_at=NULL WHERE peer=?",attempt.peer());
            else write(db,"""
                    UPDATE dzoka.federation_peers SET failures=least(failures+1,3),
                      retry_at=CASE WHEN failures>=2 THEN clock_timestamp()+interval '1 minute' ELSE retry_at END WHERE peer=?
                    """,attempt.peer());
            return null;
        });
    }

    private Discovery discover(Connection db,String peer,Query query) throws SQLException {
        var hits=new ArrayList<Candidate>();
        try(var statement=q(db,"""
                SELECT o.offer_id,o.published_month FROM dzoka.federation_offers o JOIN dzoka.reports r
                ON r.tenant_id=o.tenant_id AND r.id=o.report_id
                WHERE o.active AND r.kind='FOUND' AND r.current_revision=o.revision AND NOT r.legal_hold
                 AND r.deleted_at IS NULL AND r.expires_at>clock_timestamp()
                 AND o.category=? AND o.country=? AND o.observed_month=? ORDER BY o.published_month DESC,o.offer_id LIMIT ?
                """,query.category().name(),query.country(),period(query.month()).atDay(1),query.limit());
            var rows=statement.executeQuery()) {
            while(rows.next()) {
                UUID reference=UUID.nameUUIDFromBytes((node+"/"+peer+"/"+rows.getObject(1)).getBytes(StandardCharsets.US_ASCII));
                hits.add(new Candidate(reference,hits.size()+1,YearMonth.from(rows.getObject(2,LocalDate.class)).toString()));
            }
        }
        return new Discovery(POLICY,List.copyOf(hits));
    }

    private static PublicKey trusted(Connection db,String peer,String key,boolean inbound) throws SQLException {
        try(var query=q(db,"""
                SELECT k.public_key FROM dzoka.federation_keys k JOIN dzoka.federation_peers p
                ON p.tenant_id=k.tenant_id AND p.peer=k.peer WHERE p.peer=? AND k.key_id=? AND p.active
                AND k.revoked_at IS NULL AND p.min_version<=? AND p.max_version>=?
                AND CASE WHEN ? THEN p.inbound ELSE p.outbound END
                """,peer,key,VERSION,VERSION,inbound);var row=query.executeQuery()) {
            if(!row.next()) throw new Failures.Forbidden();
            return publicKey(row.getString(1));
        }
    }

    private static boolean allow(Connection db,String peer,String direction) throws SQLException {
        try(var query=q(db,"""
                SELECT count(*)<? FROM (SELECT 1 FROM dzoka.federation_traffic WHERE peer=? AND direction=?
                AND recorded_at>clock_timestamp()-interval '1 minute' LIMIT ?) recent
                """,BUDGET,peer,direction,BUDGET);var row=query.executeQuery()) {
            row.next();return row.getBoolean(1);
        }
    }

    public int prune(VerifiedIdentity actor,UUID scope) {
        scope(scope);
        tx.execute(actor,scope,ADMIN,db -> null);
        return tx.execute(SERVICE,scope,SERVICE_PERMISSION,db -> write(db,"""
                DELETE FROM dzoka.federation_traffic WHERE (peer,request_id,direction) IN (
                  SELECT peer,request_id,direction FROM dzoka.federation_traffic WHERE recorded_at<clock_timestamp()-interval '30 days'
                  ORDER BY recorded_at,peer,request_id,direction LIMIT 500)
                """));
    }

    private static void requirePeer(Connection db,String peer) throws SQLException {
        try(var query=q(db,"SELECT 1 FROM dzoka.federation_peers WHERE peer=?",peer);var row=query.executeQuery()) {
            if(!row.next()) throw new Failures.NotFound();
        }
    }

    private void requireNode() { if(tenant==null) throw new Failures.NotFound(); }
    private void scope(UUID scope) { requireNode();if(!tenant.equals(scope)) throw new Failures.NotFound(); }
    private void peerName(String peer) {
        identifier(peer);
        if(peer.equals(node)) throw new IllegalArgumentException("Local identity cannot be a federation peer");
    }
    private static Capabilities capabilities() { return new Capabilities(List.of(VERSION),List.of("SEARCH"),LIMIT,POLICY); }
    private static PeerResult empty(String peer,String state) { return new PeerResult(peer,state,List.of()); }
    private static YearMonth period(String value) {
        if(value==null || !value.matches("20[0-9]{2}-(0[1-9]|1[0-2])")) throw new IllegalArgumentException("Invalid month");
        return YearMonth.parse(value);
    }
    private void lock(Connection db,String peer) throws SQLException {
        try(var query=q(db,"SELECT pg_advisory_xact_lock(hashtextextended(?,20))",tenant+":"+peer)) { query.execute(); }
    }
    private static PreparedStatement q(Connection db,String sql,Object... args) throws SQLException {
        var statement=db.prepareStatement(sql);
        for(int i=0;i<args.length;i++) statement.setObject(i+1,args[i]);
        return statement;
    }
    private static int write(Connection db,String sql,Object... args) throws SQLException {
        try(var statement=q(db,sql,args)) { return statement.executeUpdate(); }
    }
}
