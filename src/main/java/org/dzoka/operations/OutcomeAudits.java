package org.dzoka.operations;

import java.util.*;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.learning.Learning;
import org.springframework.stereotype.Service;
import tools.jackson.databind.*;

/** Independent outcome evidence; never a claim/release authorization input. */
@Service
public final class OutcomeAudits {
    private final TenantTransactions tx;private final ObjectMapper json;
    public OutcomeAudits(TenantTransactions tx,ObjectMapper json){this.tx=tx;this.json=json;}
    public record Audit(int revision,String sourceSha,String ownerTruth,String fraudFinding,String contribution,String evidenceSha){
        public Audit{Learning.sha(sourceSha);Learning.sha(evidenceSha);if(revision<1||ownerTruth==null||fraudFinding==null||contribution==null||!Set.of("OWNER","NOT_OWNER","UNAVAILABLE").contains(ownerTruth)||!Set.of("CONFIRMED_FRAUD","NO_FRAUD","UNAVAILABLE").contains(fraudFinding)||!Set.of("LOCAL","FEDERATED","UNAVAILABLE").contains(contribution))throw new IllegalArgumentException();}
    }
    public JsonNode context(VerifiedIdentity actor,UUID tenant,UUID claim){return tx.execute(actor,tenant,"analytics:adjudicate",db->context(db,claim));}
    private JsonNode context(java.sql.Connection db,UUID claim)throws java.sql.SQLException{try(var s=Learning.q(db,"SELECT dzoka.outcome_context(?)::text",claim);var r=s.executeQuery()){r.next();String value=r.getString(1);if(value==null)throw new Failures.NotFound();return json.readTree(value);}}
    public void record(VerifiedIdentity actor,UUID tenant,UUID claim,UUID id,Audit value){tx.execute(actor,tenant,"analytics:adjudicate",db->{Learning.lock(db,tenant);var source=context(db,claim);
        if(!source.path("independent").asBoolean())throw new Failures.Forbidden();if(!value.sourceSha().equals(source.path("sourceSha").asString()))throw new Failures.Conflict();String artifact=json.writeValueAsString(value);
        try(var s=Learning.q(db,"SELECT claim_id,artifact::text,auditor_ref FROM dzoka.outcome_audits WHERE id=?",id);var r=s.executeQuery()){if(r.next()){if(!claim.equals(r.getObject(1,UUID.class))||!json.readTree(artifact).equals(json.readTree(r.getString(2)))||!actor.reference().equals(r.getString(3)))throw new Failures.Conflict();return null;}}
        try(var s=Learning.q(db,"SELECT coalesce(max(revision),0)+1 FROM dzoka.outcome_audits WHERE claim_id=?",claim);var r=s.executeQuery()){r.next();if(r.getInt(1)!=value.revision())throw new Failures.Conflict();}
        Learning.write(db,"INSERT INTO dzoka.outcome_audits(tenant_id,id,claim_id,revision,source_sha,owner_truth,fraud_finding,contribution,evidence_sha,auditor_ref,artifact) VALUES(?,?,?,?,?,?,?,?,?,?,?::jsonb)",tenant,id,claim,value.revision(),value.sourceSha(),value.ownerTruth(),value.fraudFinding(),value.contribution(),value.evidenceSha(),actor.reference(),artifact);return null;
    });}
}
