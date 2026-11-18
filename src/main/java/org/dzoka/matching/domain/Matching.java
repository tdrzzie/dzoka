package org.dzoka.matching.domain;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

public final class Matching {
    public static final int POOL_LIMIT = 512;
    public static final int ROUTE_LIMIT = 20;
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+", Pattern.UNICODE_CHARACTER_CLASS);
    private Matching() {}

    public enum Route { IDENTIFIER, LEXICAL, SPATIAL_TIME, EXACT_VECTOR, CATEGORY, JOINT_VECTOR }
    public enum RouteState { COMPLETE, APPROXIMATE, TRUNCATED, UNAVAILABLE, FAILED, CAPACITY_EXCEEDED }
    public record RouteStatus(Route route, RouteState state, int returned, String reason) {}
    public record Position(double latitude, double longitude, double uncertainty) {}
    public record Identifier(int keyVersion, String scopeDigest, String digest, UUID attestation) {}
    public record Vector(UUID component, String spaceKey, double[] values) {
        public Vector { values = values.clone(); }
        @Override public double[] values() { return values.clone(); }
    }
    public record Observation(UUID report, int revision, String text, Position position,
                              Instant after, Instant before, Identifier identifier, Vector vector, String category) {
        public Observation(UUID report,int revision,String text,Position position,Instant after,Instant before,
                           Identifier identifier,Vector vector) {
            this(report,revision,text,position,after,before,identifier,vector,null);
        }
    }
    public record Evidence(UUID sourceComponent, UUID candidateComponent, String spaceKey) {}
    public record JointEvidence(UUID sourceComponent,UUID candidateComponent,String spaceKey,
                                String sourceTask,String candidateTask) {}
    public record JointMatch(int revision,double similarity,JointEvidence evidence) {}
    public record JointRoute(boolean applicable,boolean failed,Map<UUID,JointMatch> matches) {
        public static final JointRoute UNAVAILABLE=new JointRoute(false,false,Map.of());
        public JointRoute { matches=Map.copyOf(matches); }
    }
    public record Candidate(UUID report, int revision, Map<Route,Integer> ranks, double fusion,
                            EvidenceScoring.Score score, List<EvidenceScoring.Signal> signals, Evidence vectorEvidence,
                            Calibration.Estimate confidence,JointEvidence jointEvidence) {
        public Candidate withConfidence(Calibration.Estimate estimate) {
            var assessed=new EvidenceScoring.Score(score.similarity(),score.coverage(),score.disposition(),
                    estimate==null?"UNCALIBRATED":"CALIBRATED");
            return new Candidate(report,revision,ranks,fusion,assessed,signals,vectorEvidence,estimate,jointEvidence);
        }
    }
    public record Search(UUID report, int revision, Long activationVersion, String scoringVersion,
                         boolean partial, boolean resultsTruncated, List<RouteStatus> routes, List<Candidate> candidates) {
        public Search withCandidates(List<Candidate> assessed) {
            return new Search(report,revision,activationVersion,scoringVersion,partial,resultsTruncated,routes,assessed);
        }
    }
    private record Assessed(Observation observation, Map<Route,Double> routes, List<EvidenceScoring.Signal> signals,
                            EvidenceScoring.Score score, Evidence vectorEvidence,JointEvidence jointEvidence) {}
    public record VectorRoute(String backend, Map<UUID,Double> similarities, boolean failed) {
        public VectorRoute { similarities=Map.copyOf(similarities); }
        public VectorRoute shortlist() {
            Map<UUID,Double> selected=new HashMap<>();
            similarities.entrySet().stream()
                    .sorted(Map.Entry.<UUID,Double>comparingByValue().reversed()
                            .thenComparing(row -> row.getKey().toString()))
                    .limit(ROUTE_LIMIT+1).forEach(row -> selected.put(row.getKey(),row.getValue()));
            return new VectorRoute(backend,selected,failed);
        }
    }
    public record IndexedRoute(String backend, Map<UUID,Integer> revisions, boolean approximate, boolean failed) {
        public IndexedRoute { revisions=Map.copyOf(revisions); }
    }

    public static VectorRoute exactVectorRoute(Observation source,List<Observation> pool) {
        Map<UUID,Double> scores=new HashMap<>();
        for (Observation row:pool) {
            Double similarity=cosine(source.vector(),row.vector());
            if (similarity!=null) scores.put(row.report(),similarity);
        }
        return new VectorRoute("EXACT_ROUTE",scores,false);
    }

    public static Search search(Observation source, List<Observation> pool, Long activationVersion, int limit) {
        return search(source,pool,activationVersion,limit,null);
    }

    public static Search search(Observation source, List<Observation> pool, Long activationVersion, int limit,
                                VectorRoute externalVector) {
        return search(source,pool,activationVersion,limit,externalVector,Map.of(),JointRoute.UNAVAILABLE);
    }

    public static Search search(Observation source, List<Observation> pool, Long activationVersion, int limit,
                                VectorRoute externalVector, Map<Route,IndexedRoute> indexedRoutes,JointRoute joint) {
        if (limit < 1 || limit > ROUTE_LIMIT) throw new IllegalArgumentException("Matching limit out of range");
        if (pool.size() > POOL_LIMIT) return overCapacity(source,activationVersion);
        Set<String> sourceTokens = tokens(source.text());
        var assessed = pool.stream().map(row ->
                compare(source,sourceTokens,row,externalVector,joint.matches().get(row.report()))).toList();
        Map<UUID,Map<Route,Integer>> ranking = new HashMap<>();
        List<RouteStatus> statuses = new ArrayList<>();
        for (Route route : Route.values()) {
            IndexedRoute indexed = indexedRoutes.get(route);
            if (route==Route.EXACT_VECTOR && externalVector!=null && externalVector.failed()) {
                statuses.add(new RouteStatus(route,RouteState.FAILED,0,externalVector.backend()+"_UNAVAILABLE"));
                continue;
            }
            if (route==Route.JOINT_VECTOR && joint.failed()) {
                statuses.add(new RouteStatus(route,RouteState.FAILED,0,"JOINT_ACTIVE_PINS_UNAVAILABLE"));
                continue;
            }
            if (!routeApplicable(source, sourceTokens, route,joint)) {
                statuses.add(new RouteStatus(route,RouteState.UNAVAILABLE,0,"SOURCE_EVIDENCE_UNAVAILABLE"));
                continue;
            }
            if (indexed!=null && indexed.failed()) {
                statuses.add(new RouteStatus(route,RouteState.FAILED,0,indexed.backend()+"_UNAVAILABLE"));
                continue;
            }
            var ordered = assessed.stream().filter(row -> row.routes().containsKey(route))
                    .filter(row -> indexed==null || Objects.equals(indexed.revisions().get(row.observation().report()),
                            row.observation().revision()))
                    .sorted(Comparator.<Assessed>comparingDouble(row -> row.routes().get(route)).reversed()
                            .thenComparing(row -> row.observation().report().toString())).toList();
            int count = Math.min(ordered.size(), ROUTE_LIMIT);
            for (int i=0; i<count; i++) {
                ranking.computeIfAbsent(ordered.get(i).observation().report(), id -> new EnumMap<>(Route.class))
                        .put(route,i+1);
            }
            boolean truncated = ordered.size()>count;
            String backend = indexed!=null ? indexed.backend()
                    : route==Route.EXACT_VECTOR && externalVector!=null ? externalVector.backend()
                    : route==Route.JOINT_VECTOR ? "JOINT_ACTIVE_PINS" : "EXACT_ROUTE";
            boolean approximate = backend.equals("PGVECTOR_HNSW") || (indexed!=null && indexed.approximate());
            if (indexed!=null && indexed.revisions().size()!=ordered.size()) approximate=true;
            RouteState state=truncated?RouteState.TRUNCATED:approximate?RouteState.APPROXIMATE:RouteState.COMPLETE;
            statuses.add(new RouteStatus(route,state,
                    count,truncated?backend+"_ROUTE_LIMIT":backend));
        }
        var fused = assessed.stream().filter(row -> ranking.containsKey(row.observation().report())).map(row -> {
            Map<Route,Integer> ranks = ranking.get(row.observation().report());
            double fusion = ranks.values().stream().mapToDouble(rank -> 1.0/(60+rank)).sum();
            return new Candidate(row.observation().report(),row.observation().revision(),Map.copyOf(ranks),
                    fusion,row.score(),row.signals(),row.vectorEvidence(),null,row.jointEvidence());
        }).sorted(Comparator.comparing((Candidate row) -> row.score().disposition()==EvidenceScoring.Disposition.CONFLICT)
                .thenComparing(Candidate::fusion,Comparator.reverseOrder()).thenComparing(Candidate::report)).toList();
        var reranked = fused.stream().limit(ROUTE_LIMIT)
                .sorted(Comparator.comparing((Candidate row) -> row.score().disposition()==EvidenceScoring.Disposition.CONFLICT)
                        .thenComparing(row -> row.score().similarity(),Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Candidate::fusion,Comparator.reverseOrder()).thenComparing(Candidate::report)).toList();
        int count = Math.min(reranked.size(),limit);
        return new Search(source.report(),source.revision(),activationVersion,EvidenceScoring.VERSION,
                statuses.stream().anyMatch(status -> status.state()!=RouteState.COMPLETE),
                fused.size()>count,List.copyOf(statuses),List.copyOf(reranked.subList(0,count)));
    }

    public static Search overCapacity(Observation source, Long activationVersion) {
        var states = Arrays.stream(Route.values()).map(route ->
                new RouteStatus(route, RouteState.CAPACITY_EXCEEDED, 0, "EXACT_POOL_LIMIT")).toList();
        return new Search(source.report(),source.revision(),activationVersion,EvidenceScoring.VERSION,true,false,states,List.of());
    }

    private static boolean routeApplicable(Observation source, Set<String> words, Route route,JointRoute joint) {
        return switch (route) {
            case IDENTIFIER -> source.identifier()!=null;
            case LEXICAL -> !words.isEmpty();
            case SPATIAL_TIME -> source.position()!=null || completeInterval(source);
            case EXACT_VECTOR -> source.vector()!=null;
            case CATEGORY -> source.category()!=null;
            case JOINT_VECTOR -> joint.applicable();
        };
    }

    private static Assessed compare(Observation source, Set<String> words, Observation target, VectorRoute vectorRoute,
                                    JointMatch joint) {
        List<EvidenceScoring.Signal> signals = new ArrayList<>();
        Map<Route,Double> routes = new EnumMap<>(Route.class);
        if (source.category()!=null && source.category().equals(target.category())) routes.put(Route.CATEGORY,1.0);
        var other = tokens(target.text());
        Double lexical = words.isEmpty() || other.isEmpty() ? null : jaccard(words,other);
        signals.add(signal("LEXICAL",EvidenceScoring.Group.DESCRIPTION,lexical,0.6));
        if (lexical!=null && lexical>0) routes.put(Route.LEXICAL,lexical);

        Double spatial = source.position()==null || target.position()==null ? null : proximity(source.position(),target.position());
        double spatialReliability = spatial==null ? 0 : 1.0/(1+(source.position().uncertainty()+target.position().uncertainty())/1000);
        signals.add(signal("SPATIAL",EvidenceScoring.Group.CONTEXT,spatial,spatialReliability));
        Double time = completeInterval(source) && completeInterval(target) ? temporal(source,target) : null;
        signals.add(signal("TEMPORAL",EvidenceScoring.Group.CONTEXT,time,0.4));
        double context = Math.max(spatial==null?0:spatial*spatialReliability,time==null?0:time*0.4);
        if (context>0) routes.put(Route.SPATIAL_TIME,context);

        Identifier left=source.identifier(),right=target.identifier();
        boolean comparable=left!=null && right!=null && left.keyVersion()==right.keyVersion()
                && left.scopeDigest().equals(right.scopeDigest());
        boolean conflict=comparable && !left.digest().equals(right.digest());
        Double identifier = !comparable ? null : (conflict ? 0.0 : 1.0);
        signals.add(signal("VERIFIED_IDENTIFIER",EvidenceScoring.Group.VERIFIED_IDENTIFIER,identifier,1));
        if (comparable && !conflict) routes.put(Route.IDENTIFIER,1.0);

        Double cosine = cosine(source.vector(),target.vector());
        signals.add(signal("TEXT_VECTOR",EvidenceScoring.Group.DESCRIPTION,cosine==null?null:Math.max(0,cosine),0.7));
        Double retrieval = vectorRoute==null ? cosine : vectorRoute.similarities().get(target.report());
        if (cosine!=null && retrieval!=null) routes.put(Route.EXACT_VECTOR,retrieval);
        Evidence evidence = cosine==null ? null : new Evidence(source.vector().component(),target.vector().component(),source.vector().spaceKey());
        JointEvidence jointEvidence=null;
        if (joint!=null && joint.revision()==target.revision()) {
            jointEvidence=joint.evidence();
            routes.put(Route.JOINT_VECTOR,joint.similarity());
            signals.add(signal("JOINT_VECTOR",EvidenceScoring.Group.DESCRIPTION,Math.max(0,joint.similarity()),0.7));
        }
        return new Assessed(target,routes,List.copyOf(signals),EvidenceScoring.score(signals,conflict),evidence,jointEvidence);
    }

    private static EvidenceScoring.Signal signal(String kind, EvidenceScoring.Group group, Double value, double reliability) {
        return new EvidenceScoring.Signal(kind,group,value==null?EvidenceScoring.State.UNAVAILABLE:EvidenceScoring.State.AVAILABLE,value,
                value==null?0:reliability,value==null?"NO_COMPARABLE_EVIDENCE":"OBSERVED");
    }

    public static Set<String> tokens(String text) {
        if (text==null) return Set.of();
        var matches = WORD.matcher(text.toLowerCase(Locale.ROOT));
        var words = new HashSet<String>();
        while(matches.find()) words.add(matches.group());
        return words;
    }

    private static double jaccard(Set<String> left, Set<String> right) {
        long common = left.stream().filter(right::contains).count();
        return (double)common/(left.size()+right.size()-common);
    }

    private static double proximity(Position a, Position b) {
        double lat = Math.toRadians(b.latitude()-a.latitude());
        double lon = Math.toRadians(b.longitude()-a.longitude());
        double hav = Math.pow(Math.sin(lat/2),2)+Math.cos(Math.toRadians(a.latitude()))*
                Math.cos(Math.toRadians(b.latitude()))*Math.pow(Math.sin(lon/2),2);
        double meters = 12_742_000*Math.asin(Math.sqrt(Math.min(1,Math.max(0,hav))));
        return Math.max(0,1-Math.max(0,meters-a.uncertainty()-b.uncertainty())/5000);
    }

    private static boolean completeInterval(Observation row) { return row.after()!=null && row.before()!=null; }

    private static double temporal(Observation a, Observation b) {
        double gap = Math.max(0,Math.max(a.after().getEpochSecond()-b.before().getEpochSecond(),
                b.after().getEpochSecond()-a.before().getEpochSecond()));
        return Math.max(0,1-gap/86400);
    }

    public static Double cosine(Vector left, Vector right) {
        if (left==null || right==null || !left.spaceKey().equals(right.spaceKey()) ||
                left.values.length!=right.values.length) return null;
        double dot=0,aa=0,bb=0;
        for (int i=0; i<left.values.length; i++) {
            double a=left.values[i],b=right.values[i];
            dot+=a*b;aa+=a*a;bb+=b*b;
        }
        double value=dot/Math.sqrt(aa*bb);
        return Double.isFinite(value) ? Math.max(-1,Math.min(1,value)) : null;
    }
}
