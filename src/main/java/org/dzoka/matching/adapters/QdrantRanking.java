package org.dzoka.matching.adapters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.dzoka.matching.domain.Matching.Observation;
import org.dzoka.matching.domain.Matching.Vector;
import org.dzoka.matching.domain.Matching.VectorRoute;
import tools.jackson.databind.ObjectMapper;

/** Queries a detached same-host projection using immutable component IDs. */
final class QdrantRanking {
    private static final String BACKEND = "QDRANT_EXACT";
    private final URI origin;
    private final String key;
    private final ObjectMapper json = new ObjectMapper();

    QdrantRanking(String endpoint, String key) {
        origin=URI.create(endpoint);
        if (!"http".equals(origin.getScheme()) || !"127.0.0.1".equals(origin.getHost()) ||
                origin.getPort()<1 || origin.getPort()>65535 || !"".equals(origin.getRawPath()) ||
                origin.getRawQuery()!=null || origin.getRawFragment()!=null || origin.getRawUserInfo()!=null ||
                !key.matches("[A-Za-z0-9_-]{24,256}")) throw new IllegalArgumentException("Private Qdrant configuration required");
        this.key=key;
    }

    VectorRoute rank(UUID tenant, Vector source, List<Observation> pool) {
        var permitted=new HashMap<UUID, UUID>();
        for (Observation row:pool) {
            if (row.vector()!=null && source.spaceKey().equals(row.vector().spaceKey())) {
                permitted.put(row.vector().component(),row.report());
            }
        }
        if (permitted.isEmpty()) return new VectorRoute(BACKEND,Map.of(),false);
        if (!source.spaceKey().matches("[0-9a-f]{64}")) return failure();
        HttpURLConnection connection=null;
        try {
            var body=Map.of("query",source.values(),"params",Map.of("exact",true),
                    "limit",permitted.size(),"with_vector",false,"with_payload",false,
                    "filter",Map.of("must",List.of(Map.of("key","tenant","match",Map.of("value",tenant.toString())),
                            Map.of("key","space","match",Map.of("value",source.spaceKey())),
                            Map.of("has_id",permitted.keySet()))));
            byte[] bytes=json.writeValueAsBytes(body);
            URI url=origin.resolve("/collections/dzoka_"+source.spaceKey()+"/points/query?timeout=1");
            connection=(HttpURLConnection)url.toURL().openConnection(Proxy.NO_PROXY);
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1500);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("api-key",key);
            connection.setRequestProperty("Content-Type","application/json");
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (var sink=connection.getOutputStream()) { sink.write(bytes); }
            if (connection.getResponseCode()!=200) return failure();
            var response=new ByteArrayOutputStream();
            long started=System.nanoTime();
            try (var input=connection.getInputStream()) {
                byte[] chunk=new byte[4096];
                for (int length; (length=input.read(chunk))!=-1;) {
                    if (response.size()+length>131072 || System.nanoTime()-started>TimeUnit.SECONDS.toNanos(2)) return failure();
                    response.write(chunk,0,length);
                }
            }
            var result=json.readTree(response.toByteArray());
            if (!"ok".equals(result.path("status").asString()) || !result.path("result").path("points").isArray()) return failure();
            var scores=new HashMap<UUID,Double>();
            for (var point:result.path("result").path("points")) {
                UUID report=permitted.get(UUID.fromString(point.path("id").asString()));
                var value=point.path("score");
                if (report==null || !value.isNumber()) return failure();
                double number=value.asDouble();
                if (!Double.isFinite(number) || Math.abs(number)>1.00001 || scores.put(report,number)!=null) return failure();
            }
            if (scores.size()!=permitted.size()) return failure();
            return new VectorRoute(BACKEND,scores,false);
        } catch (IOException | RuntimeException error) {
            return failure();
        } finally {
            if (connection!=null) connection.disconnect();
        }
    }

    private static VectorRoute failure() { return new VectorRoute(BACKEND,Map.of(),true); }

    void reconcile(UUID tenant, String space, List<Vector> vectors, String writeKey) throws IOException {
        if(!space.matches("[0-9a-f]{64}") || !writeKey.matches("[A-Za-z0-9_-]{24,256}")) {
            throw new IOException("Projection write configuration unavailable");
        }
        var retained=new ArrayList<UUID>();
        var batch=new ArrayList<Map<String,Object>>();
        String path="/collections/dzoka_"+space+"/points";
        for(Vector vector:vectors) {
            retained.add(vector.component());
            batch.add(Map.of("id",vector.component(),"vector",vector.values(),
                    "payload",Map.of("tenant",tenant.toString(),"space",space)));
            if(batch.size()==32) {
                mutate("PUT",path+"?wait=true&timeout=2",Map.of("points",batch),writeKey);
                batch.clear();
            }
        }
        if(!batch.isEmpty()) mutate("PUT",path+"?wait=true&timeout=2",Map.of("points",batch),writeKey);
        var filter=new HashMap<String,Object>();
        filter.put("must",List.of(Map.of("key","tenant","match",Map.of("value",tenant.toString())),
                Map.of("key","space","match",Map.of("value",space))));
        if(!retained.isEmpty()) filter.put("must_not",List.of(Map.of("has_id",retained)));
        mutate("POST",path+"/delete?wait=true&timeout=2",Map.of("filter",filter),writeKey);
    }

    private void mutate(String method,String path,Map<String,?> request,String writeKey) throws IOException {
        byte[] bytes=json.writeValueAsBytes(request);
        if(bytes.length>1024*1024) throw new IOException("Projection batch exceeds the transport bound");
        var connection=(HttpURLConnection)origin.resolve(path).toURL().openConnection(Proxy.NO_PROXY);
        try {
            connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(1000);connection.setReadTimeout(2500);
            connection.setRequestMethod(method);connection.setRequestProperty("api-key",writeKey);
            connection.setRequestProperty("Content-Type","application/json");connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(bytes.length);
            try(var sink=connection.getOutputStream()) { sink.write(bytes); }
            if(connection.getResponseCode()!=200) throw new IOException("Projection mutation unavailable");
            try(var body=connection.getInputStream()) {
                byte[] raw=body.readNBytes(4097);
                if(raw.length>4096) throw new IOException("Projection mutation exceeded response bound");
                var result=json.readTree(raw);
                if(!"ok".equals(result.path("status").asString()) ||
                        !"completed".equals(result.path("result").path("status").asString())) {
                    throw new IOException("Projection mutation was not acknowledged");
                }
            }
        } finally { connection.disconnect(); }
    }
}
