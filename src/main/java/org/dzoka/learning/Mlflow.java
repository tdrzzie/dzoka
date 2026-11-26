package org.dzoka.learning;

import java.sql.*;
import java.util.*;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.operations.JsonEndpoint;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/** Metadata export; native immutable registry and independent activation policy stay authoritative. */
@Service
public final class Mlflow {
    private final TenantTransactions tx;private final JsonEndpoint remote;private final String experiment;private final String backend;private final ObjectMapper json;
    public Mlflow(TenantTransactions tx,ObjectMapper json,Environment env){
        this.tx=tx;this.json=json;experiment=env.getProperty("dzoka.mlflow.experiment-id","");
        String endpoint=env.getProperty("dzoka.mlflow.origin","");backend=endpoint.isBlank()?null:Learning.hash(endpoint+":"+experiment);
        if(endpoint.isBlank()){remote=null;return;}
        if(!experiment.matches("[0-9]{1,30}"))throw new IllegalArgumentException("MLflow experiment ID required");
        remote=new JsonEndpoint(endpoint,env.getProperty("dzoka.operations.allow-insecure-loopback",Boolean.class,false),json,env.getProperty("dzoka.mlflow.token-file",""));
    }
    public boolean export(VerifiedIdentity actor,UUID tenant){
        if(remote==null)throw new Failures.Conflict();
        return tx.execute(actor,tenant,"learning:manage",db->{Learning.lock(db,tenant);
            try(var s=Learning.q(db,"SELECT e.*,d.manifest_sha,x.run_id FROM dzoka.learning_experiments e JOIN dzoka.learning_datasets d ON(d.tenant_id,d.id)=(e.tenant_id,e.dataset_id) JOIN dzoka.mlflow_exports x ON(x.tenant_id,x.experiment_id)=(e.tenant_id,e.id) WHERE x.state='PENDING' AND(x.backend_sha IS NULL OR x.backend_sha=?) AND dzoka.learning_dataset_live(e.dataset_id) AND NOT EXISTS(SELECT 1 FROM dzoka.learning_revocations v WHERE v.tenant_id=e.tenant_id AND v.kind='EXPERIMENT' AND v.entity_id=e.id) ORDER BY e.created_at,e.id LIMIT 1",backend);var r=s.executeQuery()){
                if(!r.next())return false;
                UUID id=r.getObject("id",UUID.class);Learning.write(db,"UPDATE dzoka.mlflow_exports SET backend_sha=? WHERE experiment_id=? AND backend_sha IS NULL",backend,id);String operation=Learning.hash(tenant+":"+id),run=r.getString("run_id");long timestamp=r.getTimestamp("created_at").getTime();
                try {
                    if(run==null){
                        var search=remote.post("/api/2.0/mlflow/runs/search",Map.of("experiment_ids",List.of(experiment),"filter","tags.`dzoka.operation` = '"+operation+"'","max_results",2));
                        var runs=search.path("runs");if(runs.size()>1)throw new Failures.Conflict();
                        if(runs.size()==1)run=runs.get(0).path("info").path("run_id").asString();
                        else run=remote.post("/api/2.0/mlflow/runs/create",Map.of("experiment_id",experiment,"start_time",timestamp,"tags",List.of(tag("dzoka.operation",operation),tag("dzoka.dataset.sha256",r.getString("manifest_sha")),tag("dzoka.model.sha256",r.getString("model_sha")),tag("dzoka.config.sha256",r.getString("config_sha")),tag("dzoka.evaluation.sha256",r.getString("evaluation_sha"))))).path("run").path("info").path("run_id").asString();
                        if(run==null||!run.matches("[a-zA-Z0-9-]{1,64}"))throw new Failures.Conflict();
                        Learning.write(db,"UPDATE dzoka.mlflow_exports SET run_id=? WHERE experiment_id=?",run,id);
                    }
                    var metrics=new ArrayList<Map<String,Object>>();var values=json.readTree(r.getString("metrics"));
                    for(String name:List.of("cases","available","coverage","recallAtK","recallLowerBound","mrr","singleRelevantNdcg","falseMatchRate","falseNegativeRate","brier","baselineRecallAtK"))if(values.path(name).isNumber())metrics.add(Map.of("key",name,"value",values.get(name).asDouble(),"timestamp",timestamp,"step",0));
                    remote.post("/api/2.0/mlflow/runs/log-batch",Map.of("run_id",run,"metrics",metrics));
                    remote.post("/api/2.0/mlflow/runs/update",Map.of("run_id",run,"status","FINISHED","end_time",timestamp));
                    String name="dzoka-"+Learning.hash(tenant.toString()).substring(0,24);
                    try{remote.get("/api/2.0/mlflow/registered-models/get",Map.of("name",name));}
                    catch(JsonEndpoint.StatusFailure missing){if(missing.status!=404)throw missing;remote.post("/api/2.0/mlflow/registered-models/create",Map.of("name",name,"description","Metadata references only; native Dzoka approval and artifact verification remain authoritative"));}
                    var versions=remote.get("/api/2.0/mlflow/model-versions/search",Map.of("filter","name = '"+name+"' AND run_id = '"+run+"'","max_results","2")).path("model_versions");
                    if(versions.size()>1)throw new Failures.Conflict();
                    var version=versions.size()==1?versions.get(0):remote.post("/api/2.0/mlflow/model-versions/create",Map.of("name",name,"source","dzoka://models/sha256/"+r.getString("model_sha"),"run_id",run,"tags",List.of(tag("dzoka.operation",operation),tag("dzoka.dataset.sha256",r.getString("manifest_sha")),tag("dzoka.model.sha256",r.getString("model_sha"))))).path("model_version");
                    String number=version.path("version").asString();if(number==null||!number.matches("[1-9][0-9]{0,18}"))throw new Failures.Conflict();
                    Learning.write(db,"UPDATE dzoka.mlflow_exports SET model_version=? WHERE experiment_id=?",number,id);
                    Learning.write(db,"UPDATE dzoka.mlflow_exports SET state='COMPLETE' WHERE experiment_id=?",id);return true;
                }catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new Failures.Unavailable(interrupted);}catch(java.io.IOException unavailable){throw new Failures.Unavailable(unavailable);}
            }
        });
    }
    /** Mirror a current native activation/rollback; the remote alias never grants activation authority. */
    public boolean syncActivation(VerifiedIdentity actor,UUID tenant){
        if(remote==null)throw new Failures.Conflict();
        return tx.execute(actor,tenant,"learning:manage",db->{Learning.lock(db,tenant);
            try(var s=Learning.q(db,"SELECT a.version,x.model_version,(dzoka.learning_active_allowed() AND dzoka.learning_gate_configured()) allowed FROM dzoka.fingerprint_active a LEFT JOIN dzoka.learning_experiments e ON(e.tenant_id,e.rollout_id)=(a.tenant_id,a.rollout_id) LEFT JOIN dzoka.learning_approvals p ON(p.tenant_id,p.experiment_id)=(e.tenant_id,e.id) LEFT JOIN dzoka.mlflow_exports x ON(x.tenant_id,x.experiment_id)=(e.tenant_id,e.id) AND x.state='COMPLETE' WHERE p.experiment_id IS NOT NULL AND x.backend_sha=? ORDER BY e.created_at DESC LIMIT 1",backend);var r=s.executeQuery()){
                if(!r.next())return false;String version=r.getString("model_version"),name="dzoka-"+Learning.hash(tenant.toString()).substring(0,24);
                try{
                    remote.post("/api/2.0/mlflow/registered-models/set-tag",Map.of("name",name,"key","dzoka.native.activation","value",Long.toString(r.getLong("version"))));
                    remote.post("/api/2.0/mlflow/registered-models/set-tag",Map.of("name",name,"key","dzoka.native.evidence","value",r.getBoolean("allowed")?"AVAILABLE":"SUSPENDED"));
                    if(!r.getBoolean("allowed"))return false;
                    if(version==null)throw new Failures.Conflict();remote.post("/api/2.0/mlflow/registered-models/alias",Map.of("name",name,"alias","observed-native-active","version",version));return true;
                }catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new Failures.Unavailable(interrupted);}catch(java.io.IOException unavailable){throw new Failures.Unavailable(unavailable);}
            }
        });
    }
    private static Map<String,String> tag(String key,String value){return Map.of("key",key,"value",value);}
}
