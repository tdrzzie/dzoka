package org.dzoka.fingerprints.domain;

import java.util.*;

/** A bounded, explicit evaluation cohort. Coverage does not imply similarity or model quality. */
public final class FingerprintRollout {
    private FingerprintRollout(){}
    public record Source(UUID reportId,int revision) {
        public Source {Objects.requireNonNull(reportId);if(revision<1)throw new IllegalArgumentException("Invalid source revision");}
    }
    public record PerceptionPin(UUID reportId,UUID componentId){public PerceptionPin{Objects.requireNonNull(reportId);Objects.requireNonNull(componentId);}}
    public record Plan(UUID id,List<Source> sources,List<UUID> spaces,Integer minimumCoverageBps,String evaluationSha256,List<PerceptionPin> perceptionPins) {
        public Plan(UUID id,List<Source> sources,List<UUID> spaces,Integer minimumCoverageBps,String evaluationSha256){this(id,sources,spaces,minimumCoverageBps,evaluationSha256,List.of());}
        public Plan {
            Objects.requireNonNull(id);Objects.requireNonNull(sources);Objects.requireNonNull(spaces);
            perceptionPins=perceptionPins==null?List.of():List.copyOf(perceptionPins);
            if(sources.isEmpty()||sources.size()>100||(spaces.isEmpty()&&perceptionPins.isEmpty())||spaces.size()>16||perceptionPins.size()>64
                    || minimumCoverageBps==null||minimumCoverageBps<0||minimumCoverageBps>10000||evaluationSha256==null||!evaluationSha256.matches("[a-f0-9]{64}"))
                throw new IllegalArgumentException("Invalid rollout plan");
            sources=sources.stream().sorted(Comparator.comparing(s->s.reportId().toString())).toList();
            spaces=spaces.stream().sorted(Comparator.comparing(UUID::toString)).toList();
            perceptionPins=perceptionPins.stream().sorted(Comparator.comparing((PerceptionPin p)->p.reportId().toString()).thenComparing(p->p.componentId().toString())).toList();
            if(sources.stream().map(Source::reportId).distinct().count()!=sources.size()||new HashSet<>(spaces).size()!=spaces.size())
                throw new IllegalArgumentException("Duplicate rollout source or space");
            var reports=sources.stream().map(Source::reportId).collect(java.util.stream.Collectors.toSet());
            if(perceptionPins.stream().map(PerceptionPin::componentId).distinct().count()!=perceptionPins.size()||perceptionPins.stream().anyMatch(p->!reports.contains(p.reportId())))throw new IllegalArgumentException("Invalid perception pin cohort");
        }
        public String canonical(){
            var body=new StringBuilder("fingerprint-rollout-v1\n"+minimumCoverageBps+"\n"+evaluationSha256);
            for(var source:sources)body.append('\n').append(source.reportId()).append(':').append(source.revision());
            for(var space:spaces)body.append('\n').append(space);
            for(var pin:perceptionPins)body.append("\nP:").append(pin.reportId()).append(':').append(pin.componentId());
            return body.toString();
        }
    }
    public record Status(UUID id,String state,int reports,int spaces,int expectedSlots,int availableSlots,int unavailableSlots,
            int coverageBps,int minimumCoverageBps,String evaluationSha256,String planSha256,int perceptionPins){}
    public record Activation(UUID rolloutId,long version){}
}
