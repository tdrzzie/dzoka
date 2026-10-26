package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.dzoka.fingerprints.domain.LanguageEvidence;
import org.junit.jupiter.api.Test;
class LanguageEvidenceTest {
    @Test void queryDependentScoresCannotBecomeIntrinsicFingerprintComponents(){
        UUID report=UUID.randomUUID();var component=new org.dzoka.fingerprints.domain.Perception.Component(UUID.randomUUID(),report,1,"a".repeat(64),UUID.randomUUID(),"b".repeat(64),"RERANK",java.time.Instant.now(),java.time.Instant.now(),Map.of(),Map.of(),"ACTIVE");
        assertThrows(IllegalArgumentException.class,()->new org.dzoka.fingerprints.domain.ItemFingerprint(1,UUID.randomUUID(),UUID.randomUUID(),report,1,List.of(),Map.of(),null,0,List.of(component)));
    }
    static Map<String,Object> attributes(){var fields=new HashMap<String,Object>();for(String field:LanguageEvidence.FIELDS)fields.put(field,Map.of("state","UNAVAILABLE","reason","NO_GROUNDED_SPAN","outputTruncated",false));return new HashMap<>(Map.of("fields",fields,"inputTokenCount",20,"inputTruncated",false,"representation","qa-source-spans-v1"));}
    @SuppressWarnings("unchecked")
    @Test void hallucinatedAmbiguousAndBrokenUnicodeSpansCannotBecomeFacts(){
        var e=attributes();var fields=(Map<String,Object>)e.get("fields");LanguageEvidence.validate("ATTRIBUTES",e,"black bag",Set.of());
        fields.put("colour",Map.of("state","AVAILABLE","quote","black","start",0,"end",5,"outputTruncated",false));LanguageEvidence.validate("ATTRIBUTES",e,"black bag",Set.of());
        assertThrows(IllegalArgumentException.class,()->LanguageEvidence.validate("ATTRIBUTES",e,"white bag",Set.of()));
        assertThrows(IllegalArgumentException.class,()->LanguageEvidence.validate("ATTRIBUTES",e,"black black bag",Set.of()));
        fields.put("colour",Map.of("state","UNAVAILABLE","reason","NO_GROUNDED_SPAN","outputTruncated",false));fields.put("category",Map.of("state","AVAILABLE","quote","\uDE00","start",1,"end",2,"outputTruncated",false));assertThrows(IllegalArgumentException.class,()->LanguageEvidence.validate("ATTRIBUTES",e,"\uD83D\uDE00 bag",Set.of()));
        var time=attributes();var timeFields=(Map<String,Object>)time.get("fields");timeFields.put("time",Map.of("state","AVAILABLE","quote","2pm","start",7,"end",10,"outputTruncated",false,"uncertainty","UNINTERPRETED"));
        assertThrows(IllegalArgumentException.class,()->LanguageEvidence.validate("ATTRIBUTES",time,"around 2pm",Set.of()));
        timeFields.put("time",Map.of("state","AVAILABLE","quote","around 2pm","start",0,"end",10,"outputTruncated",false,"uncertainty","UNINTERPRETED"));LanguageEvidence.validate("ATTRIBUTES",time,"around 2pm",Set.of());
    }
    @Test void rawLogitsRequireExactCandidatesAndStableOrderAndAreNotProbabilities(){
        String a=UUID.randomUUID().toString(),b=UUID.randomUUID().toString();
        var rows=new ArrayList<Map<String,Object>>();rows.add(Map.of("id",a,"rawScore",5.1,"inputTokenCount",10,"inputTruncated",false));rows.add(Map.of("id",b,"rawScore",-3.,"inputTokenCount",12,"inputTruncated",false));
        var e=Map.<String,Object>of("candidates",rows,"representation","msmarco-raw-logit-v1");LanguageEvidence.validate("RERANK",e,"",Set.of(a,b));
        assertThrows(IllegalArgumentException.class,()->LanguageEvidence.validate("RERANK",e,"",Set.of(a)));
        Collections.reverse(rows);assertThrows(IllegalArgumentException.class,()->LanguageEvidence.validate("RERANK",e,"",Set.of(a,b)));
        rows.set(0,Map.of("id",b,"rawScore",Double.NaN,"inputTokenCount",12,"inputTruncated",false));assertThrows(IllegalArgumentException.class,()->LanguageEvidence.validate("RERANK",e,"",Set.of(a,b)));
    }
}
