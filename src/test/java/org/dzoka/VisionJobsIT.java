package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.dzoka.fingerprints.domain.Perception.*;
import org.dzoka.fingerprints.ports.PerceptionJobs;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class VisionJobsIT extends AiFixture {
    static void profiles(Connection c,UUID tenant)throws Exception{
        var root=new ObjectMapper().readTree(Files.readString(Path.of("ai/vision-profiles.json")));
        for(String task:List.of("JOINT_IMAGE","JOINT_TEXT","DETECTION","SEGMENTATION"))try(var s=c.prepareStatement("INSERT INTO dzoka.perception_profiles(tenant_id,id,task,profile_key,manifest,enabled) VALUES(?,?,?,?,CAST(? AS jsonb),true)")){
            s.setObject(1,tenant);s.setObject(2,PerceptionFixtures.id(task));s.setString(3,task);s.setString(4,root.get(task).get("key").asString());s.setString(5,root.get(task).get("manifest").toString());s.executeUpdate();
        }
    }
    @Override protected void initializeProviders()throws Exception{super.initializeProviders();try(var c=admin()){profiles(c,A);profiles(c,B);}}
    private PerceptionJobs perception(){return app.getBean(PerceptionJobs.class);}
    private Job enqueue(UUID report){return perception().enqueue(who("reporter"),A,report,PerceptionFixtures.id("JOINT_TEXT"),null,null);}
    private Lease claim(){return perception().claim(who("inference"),A,PerceptionFixtures.id("JOINT_TEXT")).orElseThrow();}
    static Map<String,Object> evidence()throws Exception{
        var root=new ObjectMapper().readTree(Files.readString(Path.of("ai/vision-profiles.json")));
        var vector=new ArrayList<Double>(Collections.nCopies(768,0.));vector.set(0,1.);
        return Map.of("vector",vector,"spaceKey",root.get("JOINT_TEXT").get("manifest").get("sharedSpace").get("key").asString(),"representation","joint-embedding-v1","tokenCount",5,"truncated",false);
    }
    private Result result(Lease lease)throws Exception{return new Result(lease.lease(),lease.inputSha256(),lease.profileKey(),Instant.now(),evidence());}
    @Test void textSourceIsPrivateLeasedRevisionBoundAndNaturallyIdempotent()throws Exception{
        UUID report=report("Blue travel bag").reportId();Set<UUID> ids=new HashSet<>();
        try(var pool=Executors.newFixedThreadPool(3)){var fs=new ArrayList<Future<Job>>();for(int i=0;i<3;i++)fs.add(pool.submit(()->enqueue(report)));for(var f:fs)ids.add(f.get(20,TimeUnit.SECONDS).id());}assertEquals(1,ids.size());
        assertEquals(200,api("POST",A,"/reports/"+report+"/perception/text","reporter","{\"profileId\":\""+PerceptionFixtures.id("JOINT_TEXT")+"\"}").statusCode());
        assertThrows(Failures.NotFound.class,()->perception().read(who("other"),A,ids.iterator().next()));
        var tx=app.getBean(TenantTransactions.class);
        int visible=tx.execute(who("wrong-worker"),A,"ai:work",c->{try(var s=c.prepareStatement("SELECT count(*) FROM dzoka.report_revisions WHERE report_id=?")){s.setObject(1,report);try(var r=s.executeQuery()){r.next();return r.getInt(1);}}});assertEquals(0,visible);
        var lease=claim();assertEquals("TEXT_UTF8",lease.inputType());assertEquals("Blue travel bag",new String(perception().input(who("inference"),A,lease.jobId(),lease.lease()),StandardCharsets.UTF_8));
        assertThrows(Failures.NotFound.class,()->perception().input(who("wrong-worker"),A,lease.jobId(),lease.lease()));
        assertThrows(Failures.NotFound.class,()->perception().input(who("remote"),B,lease.jobId(),lease.lease()));
        var result=result(lease);var bad=new HashMap<>(result.evidence());bad.put("spaceKey","a".repeat(64));
        assertThrows(IllegalArgumentException.class,()->perception().complete(who("inference"),A,lease.jobId(),new Result(lease.lease(),lease.inputSha256(),lease.profileKey(),result.generatedAt(),bad)));
        assertTrue(perception().complete(who("inference"),A,lease.jobId(),result));assertTrue(perception().complete(who("inference"),A,lease.jobId(),result));
        assertEquals(1,perception().current(who("reporter"),A,report).size());revise(report,1,"Revised bag");assertTrue(perception().current(who("reporter"),A,report).isEmpty());assertFalse(perception().complete(who("inference"),A,lease.jobId(),result));
        var second=enqueue(report);var next=claim();try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.perception_jobs SET lease_until=now()-interval '1 second' WHERE tenant_id=? AND id=?")){s.setObject(1,A);s.setObject(2,second.id());s.executeUpdate();}
        assertThrows(Failures.NotFound.class,()->perception().input(who("inference"),A,second.id(),next.lease()));
    }
    @Test void wrongInputAndDisabledOrAlteredProfileCannotReserveOrPublish()throws Exception{
        UUID report=report("Bounded text").reportId();
        assertThrows(IllegalArgumentException.class,()->perception().enqueue(who("reporter"),A,report,PerceptionFixtures.id("DETECTION"),null,null));
        assertThrows(IllegalArgumentException.class,()->perception().enqueue(who("reporter"),A,report,PerceptionFixtures.id("JOINT_TEXT"),null,PerceptionFixtures.audio(0)));
        var queued=enqueue(report);var lease=claim();
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.perception_profiles SET enabled=false WHERE tenant_id=? AND id=?")){s.setObject(1,A);s.setObject(2,lease.profileId());s.executeUpdate();}
        try{assertThrows(Failures.Unavailable.class,()->perception().complete(who("inference"),A,queued.id(),result(lease)));assertThrows(Failures.NotFound.class,()->perception().input(who("inference"),A,queued.id(),lease.lease()));}
        finally{try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.perception_profiles SET enabled=true WHERE tenant_id=? AND id=?")){s.setObject(1,A);s.setObject(2,lease.profileId());s.executeUpdate();}}
    }
    private static Object sorted(Object value){if(value instanceof Map<?,?> m){var out=new TreeMap<String,Object>();m.forEach((k,v)->out.put((String)k,sorted(v)));return out;}if(value instanceof List<?> l)return l.stream().map(VisionJobsIT::sorted).toList();return value;}
    @SuppressWarnings("unchecked")
    @Test void independentlyRehashedManifestCannotHideBrokenSharedSpaceDefinition()throws Exception{
        UUID report=report("Shared identity").reportId();var codec=new ObjectMapper();String original,key;
        try(var c=admin();var s=c.prepareStatement("SELECT manifest::text,profile_key FROM dzoka.perception_profiles WHERE tenant_id=? AND id=?")){
            s.setObject(1,A);s.setObject(2,PerceptionFixtures.id("JOINT_TEXT"));try(var r=s.executeQuery()){r.next();original=r.getString(1);key=r.getString(2);}
        }
        Map<String,Object> manifest=codec.readValue(original,Map.class);((Map<String,Object>)manifest.get("sharedSpace")).put("key","f".repeat(64));
        String changed=codec.writeValueAsString(sorted(manifest));String changedKey=org.dzoka.foundation.application.ContentDigest.sha256(changed.getBytes(StandardCharsets.UTF_8));
        try{
            try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.perception_profiles SET manifest=CAST(? AS jsonb),profile_key=? WHERE tenant_id=? AND id=?")){s.setString(1,changed);s.setString(2,changedKey);s.setObject(3,A);s.setObject(4,PerceptionFixtures.id("JOINT_TEXT"));s.executeUpdate();}
            assertThrows(Failures.Unavailable.class,()->enqueue(report));
        }finally{try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.perception_profiles SET manifest=CAST(? AS jsonb),profile_key=? WHERE tenant_id=? AND id=?")){s.setString(1,original);s.setString(2,key);s.setObject(3,A);s.setObject(4,PerceptionFixtures.id("JOINT_TEXT"));s.executeUpdate();}}
    }
}
