package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.dzoka.edge.EdgeInvestigations;
import org.dzoka.edge.RecoveryIntelligence;
import org.dzoka.foundation.application.Failures;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.ReportDraft;
import org.junit.jupiter.api.Test;

class EdgeIT extends IntegrationFixture {
    @Override protected void initializeProviders() throws Exception {
        membership(A,"edge-reviewer","edge:review","reports:read","reports:read:any");
        membership(A,"investigator","edge:investigate","reports:read","reports:write");
        membership(A,"edge-worker","edge:process","reports:read","reports:read:any");
        membership(A,"analyst","intelligence:read","custody:operate","recovery:manage","reports:write");
        membership(B,"investigator","edge:investigate","reports:read");
        membership(B,"analyst","intelligence:read","custody:operate");
    }
    private VerifiedIdentity actor(String subject) { return new VerifiedIdentity(issuer,subject); }
    @Test void realIsolatedFrameWorkerUsesCameraLeaseAndSubmitsOnlyMetadata() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue("true".equals(System.getenv("DZOKA_TEST_EDGE_SANDBOX")));
        var json=new tools.jackson.databind.ObjectMapper();
        var directory=java.nio.file.Files.createTempDirectory(java.nio.file.Path.of(".test-tmp"),"edge-fixture-").toAbsolutePath();
        UUID camera=UUID.randomUUID(),grant=UUID.randomUUID(),job=UUID.randomUUID();
        Instant start=Instant.now().minusSeconds(60).truncatedTo(ChronoUnit.SECONDS);
        var entries=new ArrayList<Map<String,Object>>();
        byte[] reference=null;
        for(int frame=0;frame<3;frame++) {
            byte[] pixels=new byte[24*18*3];Arrays.fill(pixels,(byte)220);
            if(frame<2) for(int y=4;y<10;y++) for(int x=4;x<10;x++) {
                int offset=(y*24+x)*3;
                pixels[offset]=(byte)(x*31%255);pixels[offset+1]=(byte)(y*29%255);pixels[offset+2]=(byte)(x*y*11%255);
            }
            var raw=new java.io.ByteArrayOutputStream();raw.write("P6\n24 18\n255\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII));raw.write(pixels);
            String name="frame-"+frame+".ppm";
            java.nio.file.Files.write(directory.resolve(name),raw.toByteArray());
            if(frame==0) reference=raw.toByteArray();
            entries.add(Map.of("time",start.plusSeconds(frame*10).toString(),"name",name,"sha256",
                    org.dzoka.foundation.application.ContentDigest.sha256(raw.toByteArray())));
        }
        byte[] manifest=json.writeValueAsBytes(Map.of("schema",1,"camera",camera,"frames",entries));
        byte[] plan=json.writeValueAsBytes(Map.of("camera",camera,"referenceSha",org.dzoka.foundation.application.ContentDigest.sha256(reference),
                "referenceBox",List.of(4,4,6,6),"searchBox",List.of(0,0,24,18),"threshold",0.04,"purpose","LOST_PROPERTY_SEARCH",
                "disclosureMask",List.of("000000","011110","011110","011110","011110","000000")));
        java.nio.file.Files.write(directory.resolve("manifest.json"),manifest);
        java.nio.file.Files.write(directory.resolve("plan.json"),plan);
        UUID report=app.getBean(IntakeService.class).submit(actor("investigator"),A,null,null,
                new ReportDraft(ReportDraft.Kind.LOST,"Synthetic camera worker","en","UTC",null,null,null,null,null,null,null),
                new byte[]{'{','}'},UUID.randomUUID().toString()).reportId();
        var edge=app.getBean(EdgeInvestigations.class);
        edge.camera(actor("edge-reviewer"),A,camera,org.dzoka.foundation.application.ContentDigest.sha256(manifest),true);
        edge.grant(actor("edge-reviewer"),A,grant,new EdgeInvestigations.Grant(report,camera,actor("investigator").reference(),
                start,start.plusSeconds(30),Instant.now().plusSeconds(300),3,org.dzoka.foundation.application.ContentDigest.sha256(plan)));
        edge.submit(actor("investigator"),A,job,grant);
        var command=new ProcessBuilder("python","-B","tools/edge_worker.py","--base",base,"--tenant",A.toString(),
                "--archive",directory.toString(),"--manifest",directory.resolve("manifest.json").toString(),
                "--reference",directory.resolve("frame-0.ppm").toString(),"--plan",directory.resolve("plan.json").toString());
        command.environment().put("DZOKA_EDGE_TOKEN",token("edge-worker"));
        var log=directory.resolve("worker.log");
        var process=command.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        assertTrue(process.waitFor(60,java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(0,process.exitValue(),java.nio.file.Files.readString(log));
        var result=edge.read(actor("investigator"),A,job);
        assertEquals("COMPLETE",result.state());
        assertEquals(3,result.observations().size());
        assertEquals("NOT_OBSERVED",result.observations().getLast().status());
        assertFalse(result.observations().getLast().gap());
        assertFalse(result.observations().getFirst().gap());
        assertEquals(1,result.windows().size());
        assertEquals(start.plusSeconds(10),result.windows().getFirst().after());
        assertEquals(start.plusSeconds(20),result.windows().getFirst().before());
        assertEquals("POSSIBLE_DISAPPEARANCE",result.windows().getFirst().hypothesis());
        try(var db=admin();var query=db.prepareStatement("SELECT id FROM dzoka.edge_evidence WHERE job_id=? ORDER BY created_at,id")) {
            query.setObject(1,job);
            try(var row=query.executeQuery()) {
                assertTrue(row.next());
                UUID evidence=row.getObject(1,UUID.class);
                assertThrows(Failures.NotFound.class,()->edge.evidence(actor("investigator"),A,job,evidence,false));
                var crop=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(edge.evidence(actor("edge-reviewer"),A,job,evidence,true)));
                assertEquals(0,crop.getRGB(0,0)&0xffffff);
                assertNotEquals(0,crop.getRGB(2,2)&0xffffff);
            }
        }
    }
    @Test void grantRevocationStopsCompletionAndReadAndPreservesTenantIsolation() throws Exception {
        var service=app.getBean(EdgeInvestigations.class);
        var investigator=actor("investigator");
        UUID report=app.getBean(IntakeService.class).submit(investigator,A,null,null,
                new ReportDraft(ReportDraft.Kind.LOST,"Synthetic edge item","en","UTC",null,null,null,null,null,null,null),
                new byte[]{'{','}'},UUID.randomUUID().toString()).reportId();
        Instant now=Instant.now().truncatedTo(ChronoUnit.SECONDS);
        UUID camera=UUID.randomUUID(),grant=UUID.randomUUID(),job=UUID.randomUUID();
        service.camera(actor("edge-reviewer"),A,camera,"d".repeat(64),true);
        var scope=new EdgeInvestigations.Grant(report,camera,investigator.reference(),now.minusSeconds(60),now,
                now.plusSeconds(600),2,"a".repeat(64));
        service.grant(actor("edge-reviewer"),A,grant,scope);
        service.grant(actor("edge-reviewer"),A,grant,scope);
        service.submit(investigator,A,job,grant);
        service.submit(investigator,A,job,grant);
        assertThrows(Failures.Conflict.class,()->service.submit(investigator,A,UUID.randomUUID(),grant));
        assertThrows(Failures.NotFound.class,()->service.submit(investigator,B,UUID.randomUUID(),grant));
        var lease=service.claim(actor("edge-worker"),A);
        assertEquals("d".repeat(64),service.access(actor("edge-worker"),A,job,lease.token()).archiveSha());
        assertEquals(camera,lease.camera());
        var observations=List.of(new EdgeInvestigations.Observation(now.minusSeconds(30),now.minusSeconds(20),true,"b".repeat(64),"c".repeat(64)));
        service.revoke(actor("edge-reviewer"),A,grant);
        assertThrows(Failures.Conflict.class,()->service.access(actor("edge-worker"),A,job,lease.token()));
        assertFalse(service.complete(actor("edge-worker"),A,job,new EdgeInvestigations.Completion(lease.token(),observations)));
        assertThrows(Failures.Conflict.class,()->service.read(investigator,A,job));
        assertNull(service.claim(actor("edge-worker"),A));

        UUID valid=UUID.randomUUID(),next=UUID.randomUUID();
        service.grant(actor("edge-reviewer"),A,valid,scope);
        service.submit(investigator,A,next,valid);
        var allowed=service.claim(actor("edge-worker"),A);
        UUID evidence=UUID.randomUUID();
        service.evidence(actor("edge-worker"),A,next,evidence,new EdgeInvestigations.Crop(allowed.token(),1,1,new byte[]{12,34,56}));
        var json=new tools.jackson.databind.ObjectMapper();
        var maximumCrop=new EdgeInvestigations.Crop(allowed.token(),64,64,new byte[64*64*3]);
        assertEquals(204,api("PUT",A,"/edge/jobs/"+next+"/evidence/"+UUID.randomUUID(),"edge-worker",
                json.writeValueAsString(maximumCrop)).statusCode());
        assertThrows(Failures.NotFound.class,()->service.evidence(investigator,A,next,evidence,false));
        assertTrue(service.evidence(actor("edge-reviewer"),A,next,evidence,true).length>0);
        service.approveEvidence(actor("edge-reviewer"),A,next,evidence,"e".repeat(64));
        assertTrue(service.evidence(investigator,A,next,evidence,false).length>0);
        assertThrows(IllegalArgumentException.class,()->service.complete(actor("edge-worker"),A,next,
                new EdgeInvestigations.Completion(allowed.token(),List.of(observations.getFirst(),observations.getFirst(),observations.getFirst()))));
        assertTrue(service.complete(actor("edge-worker"),A,next,new EdgeInvestigations.Completion(allowed.token(),observations)));
        assertEquals("COMPLETE",service.read(investigator,A,next).state());
        assertTrue(service.read(investigator,A,next).observations().getFirst().gap());
        assertThrows(Failures.Conflict.class,()->service.complete(actor("edge-worker"),A,next,
                new EdgeInvestigations.Completion(allowed.token(),observations)));
        assertThrows(Failures.NotFound.class,()->service.read(investigator,B,next));
        service.revoke(actor("edge-reviewer"),A,valid);
        assertThrows(Failures.Conflict.class,()->service.evidence(investigator,A,next,evidence,false));
        assertEquals(1,service.purge(actor("edge-worker"),A));
        assertEquals(0,service.purge(actor("edge-worker"),A));
    }
    @Test void baselineHasNoUnsupportedProbabilityOrFutureEvidence() throws Exception {
        var intelligence=app.getBean(RecoveryIntelligence.class);
        var cutoff=Instant.now().minusSeconds(60);
        var baseline=intelligence.baseline(actor("analyst"),A,UUID.randomUUID(),UUID.randomUUID(),cutoff);
        assertEquals("UNAVAILABLE",baseline.state());
        assertTrue(baseline.transitions().isEmpty());
        assertThrows(IllegalArgumentException.class,()->intelligence.history(actor("analyst"),A,UUID.randomUUID(),Instant.now().plusSeconds(60)));
        assertThrows(Failures.Forbidden.class,()->intelligence.history(actor("investigator"),A,UUID.randomUUID(),cutoff));
        UUID source=UUID.randomUUID(),destination=UUID.randomUUID();
        var intakes=new ArrayList<UUID>();
        for(int index=0;index<3;index++) {
            UUID report=app.getBean(IntakeService.class).submit(actor("analyst"),A,null,null,
                    new ReportDraft(ReportDraft.Kind.FOUND,"Synthetic transition fixture","en","UTC",null,null,null,null,null,null,null),
                    new byte[]{'{','}'},UUID.randomUUID().toString()).reportId();
            UUID intake=UUID.randomUUID();
            intakes.add(intake);
            app.getBean(org.dzoka.recovery.ports.RecoveryOperations.class).enroll(actor("analyst"),A,intake,report,source);
            try(var db=admin();var insert=db.prepareStatement("""
                    INSERT INTO dzoka.staff_custody_events(tenant_id,intake_id,id,actor_ref,recipient_ref,source_site,destination,
                      expected_generation,observed_at,recorded_at,kind,command,evidence_sha)
                    VALUES(?,?,?,?,?,?,?,1,?,?,'STORED','STORE',?),
                          (?,?,?,?,?,?,?,2,?,?,'TRANSFER_ACCEPTED','ACCEPT_TRANSFER',?)
                    """)) {
                int position=1;
                for(int event=0;event<2;event++) {
                    insert.setObject(position++,A);insert.setObject(position++,intake);insert.setObject(position++,UUID.randomUUID());
                    insert.setString(position++,actor("analyst").reference());insert.setString(position++,actor("analyst").reference());
                    insert.setObject(position++,source);insert.setObject(position++,event==0?source:destination);
                    var instant=cutoff.minusSeconds(event==0?90:60);
                    insert.setTimestamp(position++,java.sql.Timestamp.from(instant));
                    insert.setTimestamp(position++,java.sql.Timestamp.from(instant.plusSeconds(1)));
                    insert.setString(position++,"a".repeat(64));
                }
                insert.executeUpdate();
            }
        }
        var supported=intelligence.baseline(actor("analyst"),A,UUID.randomUUID(),source,cutoff);
        assertEquals("DESCRIPTIVE",supported.state());
        assertEquals(3,supported.transitions().getFirst().observations());
        assertEquals(30.0,supported.transitions().getFirst().medianElapsedSeconds());
        assertEquals("UNAVAILABLE",intelligence.baseline(actor("analyst"),A,intakes.getFirst(),source,cutoff).state());
        assertEquals(2,intelligence.history(actor("analyst"),A,intakes.getFirst(),cutoff).size());
        assertTrue(intelligence.history(actor("analyst"),B,intakes.getFirst(),cutoff).isEmpty());
        assertTrue(intelligence.history(actor("analyst"),A,intakes.getFirst(),cutoff.minusSeconds(100)).isEmpty());
    }
}
